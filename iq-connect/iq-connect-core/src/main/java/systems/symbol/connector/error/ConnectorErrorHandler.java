package systems.symbol.connector.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Connector Error Handler — Centralized error handling, retry logic, and error callbacks.
 *
 * <p>Provides standardized error handling for connectors:
 * - Error classification (transient vs permanent)
 * - Exponential backoff retry strategy
 * - Error context tracking
 * - Event callbacks for error, warning, retry
 * - Dead-letter queue for permanently failed items
 *
 * <p>Usage:
 * <pre>
 * ConnectorErrorHandler handler = ConnectorErrorHandler.forConnector("aws-s3");
 * handler.onError(err -> log.error("Critical: {}", err.message));
 * handler.onRetry(err -> log.warn("Retrying: {}", err.itemId));
 * 
 * if (handler.isRetryable(exception)) {
 * long backoffMs = handler.getBackoffMs(retryAttempt);
 * Thread.sleep(backoffMs);
 * retry();
 * } else {
 * handler.addToDeadLetterQueue(itemId, exception);
 * }
 * </pre>
 */
public class ConnectorErrorHandler {

private static final Logger log = LoggerFactory.getLogger(ConnectorErrorHandler.class);

private final String connectorId;
private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
private final List<ConnectorError> errors = Collections.synchronizedList(new ArrayList<>());
private final List<String> deadLetterQueue = Collections.synchronizedList(new ArrayList<>());
private final Map<String, Consumer<ConnectorError>> errorHandlers = new ConcurrentHashMap<>();

private static final int MAX_STORED_ERRORS = 1000;
private static final long INITIAL_BACKOFF_MS = 1000;  // 1 second
private static final long MAX_BACKOFF_MS = 60_000;// 1 minute
private static final double BACKOFF_MULTIPLIER = 2.0;

private ConnectorErrorHandler(String connectorId) {
this.connectorId = connectorId;
}

/**
 * Create an error handler for a connector.
 */
public static ConnectorErrorHandler forConnector(String connectorId) {
return new ConnectorErrorHandler(connectorId);
}

/**
 * Check if an exception is retryable (transient).
 *
 * <p>Transient errors (retryable):
 * - Temporary network issues (timeout, connection refused, etc.)
 * - Rate limiting (HTTP 429)
 * - Service unavailable (HTTP 503)
 * - Resource temporarily unavailable
 *
 * <p>Permanent errors (not retryable):
 * - Authentication failures (401, 403)
 * - Not found (404)
 * - Invalid request (400, 422)
 * - Schema/type mismatches
 */
public boolean isRetryable(Exception ex) {
if (ex == null) return false;

String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
String className = ex.getClass().getSimpleName();

// Network errors are retryable
if (className.contains("IOException") ||
className.contains("TimeoutException") ||
className.contains("ConnectException")) {
return true;
}

// Rate limiting and service unavailable are retryable
if (msg.contains("429") || msg.contains("rate limit") ||
msg.contains("503") || msg.contains("service unavailable")) {
return true;
}

// Authentication, validation, not-found are permanent
if (msg.contains("401") || msg.contains("403") || msg.contains("unauthorized") ||
msg.contains("404") || msg.contains("not found") ||
msg.contains("400") || msg.contains("invalid")) {
return false;
}

// Default: assume transient
return true;
}

/**
 * Get exponential backoff time for a retry attempt.
 *
 * <p>Formula: min(initialBackoff * multiplier^attempt, maxBackoff)
 * <br>With randomization to prevent thundering herd.
 */
public long getBackoffMs(int attemptNumber) {
long backoff = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber));
backoff = Math.min(backoff, MAX_BACKOFF_MS);

// Add 0-20% randomization
long jitter = (long) (backoff * (0.2 * Math.random()));
return backoff + jitter;
}

/**
 * Record an error.
 */
public void recordError(String itemId, Exception exception) {
ConnectorError err = new ConnectorError();
err.connectorId = connectorId;
err.itemId = itemId;
err.exception = exception;
err.message = exception.getMessage();
err.className = exception.getClass().getSimpleName();
err.timestamp = System.currentTimeMillis();
err.retryable = isRetryable(exception);

errors.add(err);
if (errors.size() > MAX_STORED_ERRORS) {
errors.remove(0);  // Remove oldest
}

errorCounts.merge(itemId, 1, Integer::sum);
log.warn("[{}] error on {}: {} (retryable={})", 
 connectorId, itemId, err.message, err.retryable);

// Trigger registered error handler
if (errorHandlers.containsKey("error")) {
errorHandlers.get("error").accept(err);
}
}

/**
 * Add item to dead-letter queue (permanently failed).
 */
public void addToDeadLetterQueue(String itemId, Exception reason) {
deadLetterQueue.add(itemId);
log.error("[{}] item {} added to DLQ: {}", connectorId, itemId, reason.getMessage());

if (errorHandlers.containsKey("dlq")) {
ConnectorError err = new ConnectorError();
err.itemId = itemId;
err.message = reason.getMessage();
errorHandlers.get("dlq").accept(err);
}
}

/**
 * Register a handler callback.
 *
 * <p>Supported handler types:
 * - "error": called on any error
 * - "retry": called on retry attempt
 * - "dlq": called when item added to dead-letter queue
 */
public void on(String handlerType, Consumer<ConnectorError> handler) {
errorHandlers.put(handlerType, handler);
}

/**
 * Get all recorded errors.
 */
public List<ConnectorError> getErrors() {
return new ArrayList<>(errors);
}

/**
 * Get error count for an item.
 */
public int getErrorCount(String itemId) {
return errorCounts.getOrDefault(itemId, 0);
}

/**
 * Get dead-letter queue items.
 */
public List<String> getDeadLetterQueue() {
return new ArrayList<>(deadLetterQueue);
}

/**
 * Clear all errors and DLQ.
 */
public void clearHistory() {
errors.clear();
deadLetterQueue.clear();
errorCounts.clear();
log.info("[{}] cleared error history", connectorId);
}

/**
 * Error details container.
 */
public static class ConnectorError {
public String connectorId;
public String itemId;
public Exception exception;
public String message;
public String className;
public long timestamp;
public boolean retryable;

@Override
public String toString() {
return String.format(
"ConnectorError{item=%s, class=%s, msg=%s, retryable=%s}",
itemId, className, message, retryable
);
}
}
}
