package systems.symbol.connector.state;

import systems.symbol.connector.persistence.ConnectorCheckpoint.Checkpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Connector State Manager — Track runtime state of connector execution.
 *
 * <p>Manages the transient state of a connector during a sync operation:
 * - Start time, current progress, status
 * - Error tracking and retry counts
 * - Rate limiting and backoff
 * - Batch tracking (what was sent, what failed)
 *
 * <p>This is NOT persisted; it's in-memory during the connector's active session.
 * For persistent checkpoints, use {@link systems.symbol.connector.persistence.ConnectorCheckpoint}.
 *
 * <p>Usage:
 * <pre>
 * ConnectorState state = ConnectorState.start("aws-s3");
 * 
 * for (String item : items) {
 * try {
 * processItem(item);
 * state.recordSuccess();
 * } catch (Exception ex) {
 * state.recordFailure(item, ex.getMessage());
 * if (!state.canRetry(item)) {
 * log.warn("Max retries exceeded for {}", item);
 * }
 * }
 * }
 * 
 * ConnectorStats stats = state.finish();
 * log.info("Sync completed: {}", stats);
 * </pre>
 */
public class ConnectorState {

private static final Logger log = LoggerFactory.getLogger(ConnectorState.class);

private final String connectorId;
private final Instant startTime;
private final AtomicLong successCount = new AtomicLong(0);
private final AtomicLong failureCount = new AtomicLong(0);
private final Map<String, Integer> retryCount = new ConcurrentHashMap<>();
private final Map<String, String> lastError = new ConcurrentHashMap<>();
private final List<String> processedIds = Collections.synchronizedList(new ArrayList<>());
private final Map<String, Long> processingTimes = new ConcurrentHashMap<>();

private volatile String currentItemId;
private volatile String status = "running";  // "running", "paused", "completed", "failed"
private volatile Instant lastHeartbeat;
private volatile String errorMessage;

private static final int MAX_RETRIES = 3;
private static final long HEARTBEAT_TIMEOUT_MS = 60_000;  // 1 minute

private ConnectorState(String connectorId) {
this.connectorId = connectorId;
this.startTime = Instant.now();
this.lastHeartbeat = startTime;
}

/**
 * Start a new connector state session.
 */
public static ConnectorState start(String connectorId) {
log.info("[ConnectorState] started session for {}", connectorId);
return new ConnectorState(connectorId);
}

/**
 * Record successful processing of an item.
 */
public void recordSuccess() {
successCount.incrementAndGet();
if (currentItemId != null) {
processedIds.add(currentItemId);
processingTimes.put(currentItemId, System.currentTimeMillis());
}
heartbeat();
}

/**
 * Record failure processing an item.
 */
public void recordFailure(String itemId, String error) {
failureCount.incrementAndGet();
retryCount.merge(itemId, 1, Integer::sum);
lastError.put(itemId, error);
log.warn("[ConnectorState] failure for {}: {} (attempt {})", 
 itemId, error, retryCount.get(itemId));
heartbeat();
}

/**
 * Check if an item can be retried.
 */
public boolean canRetry(String itemId) {
int attempts = retryCount.getOrDefault(itemId, 0);
return attempts < MAX_RETRIES;
}

/**
 * Get the current retry count for an item.
 */
public int getRetryCount(String itemId) {
return retryCount.getOrDefault(itemId, 0);
}

/**
 * Set the item currently being processed.
 */
public void setCurrentItem(String itemId) {
this.currentItemId = itemId;
}

/**
 * Update heartbeat (prevent timeout detection).
 */
public void heartbeat() {
this.lastHeartbeat = Instant.now();
}

/**
 * Check if connector has timed out (no heartbeat for N seconds).
 */
public boolean isTimedOut() {
long elapsedMs = System.currentTimeMillis() - lastHeartbeat.toEpochMilli();
return elapsedMs > HEARTBEAT_TIMEOUT_MS;
}

/**
 * Pause the connector.
 */
public void pause() {
this.status = "paused";
log.info("[ConnectorState] paused for {}", connectorId);
}

/**
 * Resume the connector.
 */
public void resume() {
this.status = "running";
heartbeat();
log.info("[ConnectorState] resumed for {}", connectorId);
}

/**
 * Mark as failed with error message.
 */
public void fail(String message) {
this.status = "failed";
this.errorMessage = message;
log.error("[ConnectorState] failed for {}: {}", connectorId, message);
}

/**
 * Mark as successfully completed.
 */
public void complete() {
this.status = "completed";
log.info("[ConnectorState] completed for {} (success={}, failure={})", 
 connectorId, successCount.get(), failureCount.get());
}

/**
 * Finish and return statistics.
 */
public ConnectorStats finish() {
if (!status.equals("completed") && !status.equals("failed")) {
complete();
}

ConnectorStats stats = new ConnectorStats();
stats.connectorId = connectorId;
stats.successCount = successCount.get();
stats.failureCount = failureCount.get();
stats.totalCount = stats.successCount + stats.failureCount;
stats.startTime = startTime;
stats.endTime = Instant.now();
stats.durationMs = System.currentTimeMillis() - startTime.toEpochMilli();
stats.status = status;
stats.processedIds = new ArrayList<>(processedIds);
stats.failedItems = new HashMap<>(lastError);

return stats;
}

/**
 * Connector execution statistics.
 */
public static class ConnectorStats {
public String connectorId;
public long successCount;
public long failureCount;
public long totalCount;
public Instant startTime;
public Instant endTime;
public long durationMs;
public String status;
public List<String> processedIds;
public Map<String, String> failedItems;

public double getSuccessRate() {
return totalCount == 0 ? 0.0 : (successCount / (double) totalCount) * 100;
}

@Override
public String toString() {
return String.format(
"ConnectorStats{connector=%s, total=%d, success=%d (%.1f%%), failure=%d, duration=%dms, status=%s}",
connectorId, totalCount, successCount, getSuccessRate(), failureCount, durationMs, status
);
}
}
}
