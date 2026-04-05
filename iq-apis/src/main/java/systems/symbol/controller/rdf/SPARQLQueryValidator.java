package systems.symbol.controller.rdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.***REMOVED***.Pattern;

/**
 * Validates SPARQL queries for the FedX REST API.
 * 
 * Enforces:
 * - Query size limits (prevent DoS via large payloads)
 * - Timeout limits (prevent unbounded long-running queries)
 * - Valid SPARQL syntax patterns
 * - Query type validation (SELECT, ASK, CONSTRUCT, DESCRIBE)
 * - Prevention of potentially dangerous operations
 */
public class SPARQLQueryValidator {

private static final Logger log = LoggerFactory.getLogger(SPARQLQueryValidator.class);

// Configuration limits
public static final int MAX_QUERY_SIZE = 1024 * 1024;  // 1 MB
public static final int MIN_QUERY_SIZE = 10;   // "SELECT ?a WHERE { }"
public static final int DEFAULT_TIMEOUT_SECONDS = 30;
public static final int MAX_TIMEOUT_SECONDS = 300; // 5 minutes
public static final int MIN_TIMEOUT_SECONDS = 1;

// SPARQL query type patterns
private static final Pattern QUERY_TYPE_PATTERN = Pattern.compile(
"^\\s*(SELECT|ASK|CONSTRUCT|DESCRIBE|INSERT|DELETE|LOAD|CLEAR|DROP)\\s",
Pattern.CASE_INSENSITIVE | Pattern.DOTALL
);

// Pattern for basic SPARQL syntax validation
private static final Pattern BASIC_SYNTAX_PATTERN = Pattern.compile(
"^\\s*(SELECT|ASK|CONSTRUCT|DESCRIBE)\\s+.*\\s+WHERE\\s*\\{",
Pattern.CASE_INSENSITIVE | Pattern.DOTALL
);

// Potentially dangerous SPARQL operations to block
private static final Pattern DANGEROUS_OPERATIONS = Pattern.compile(
"(?i)(INSERT\\s+DATA|DELETE\\s+DATA|LOAD\\s+|CLEAR\\s+|DROP\\s+)",
Pattern.CASE_INSENSITIVE
);

/**
 * Validates a SPARQL query with all configured checks.
 * 
 * @param query the SPARQL query to validate
 * @param timeout optional timeout in seconds (will be validated)
 * @return ValidationResult containing status and detailed error messages
 */
public static ValidationResult validate(String query, Integer timeout) {
Objects.requireNonNull(query, "query must not be null");

// Check: Non-empty
if (query.trim().isEmpty()) {
return ValidationResult.error("Query is empty");
}

// Check: Size limits
if (query.length() < MIN_QUERY_SIZE) {
return ValidationResult.error(
"Query is too small (" + query.length() + " chars). " +
"Minimum size is " + MIN_QUERY_SIZE + " chars."
);
}

if (query.length() > MAX_QUERY_SIZE) {
return ValidationResult.error(
"Query exceeded maximum size (" + query.length() + " chars). " +
"Maximum allowed is " + MAX_QUERY_SIZE + " chars. " +
"Consider using a database dump or streaming API instead."
);
}

// Check: Valid query type
if (!QUERY_TYPE_PATTERN.matcher(query).find()) {
return ValidationResult.error(
"Query does not start with a valid SPARQL operation. " +
"Supported: SELECT, ASK, CONSTRUCT, DESCRIBE"
);
}

// Check: No dangerous operations
if (DANGEROUS_OPERATIONS.matcher(query).find()) {
return ValidationResult.error(
"Query contains write or data manipulation operations (INSERT, DELETE, LOAD, CLEAR, DROP). " +
"Only SELECT, ASK, CONSTRUCT, and DESCRIBE queries are supported."
);
}

// Check: Basic syntax
String trimmed = query.trim().toUpperCase();
if (!trimmed.startsWith("SELECT") && 
!trimmed.startsWith("ASK") && 
!trimmed.startsWith("CONSTRUCT") && 
!trimmed.startsWith("DESCRIBE")) {
return ValidationResult.error(
"Query must be a SELECT, ASK, CONSTRUCT, or DESCRIBE query"
);
}

// Check: Timeout validation
ValidationResult timeoutValidation = validateTimeout(timeout);
if (!timeoutValidation.isValid()) {
return timeoutValidation;
}

return ValidationResult.success(timeoutValidation.getRecommendedTimeout());
}

/**
 * Validates the timeout parameter.
 * 
 * @param timeout optional timeout in seconds
 * @return ValidationResult with validated/recommended timeout
 */
public static ValidationResult validateTimeout(Integer timeout) {
if (timeout == null) {
return ValidationResult.success(DEFAULT_TIMEOUT_SECONDS);
}

if (timeout < MIN_TIMEOUT_SECONDS) {
return ValidationResult.error(
"Timeout must be at least " + MIN_TIMEOUT_SECONDS + " second(s). " +
"Requested: " + timeout + " seconds"
);
}

if (timeout > MAX_TIMEOUT_SECONDS) {
return ValidationResult.error(
"Timeout exceeds maximum allowed (" + MAX_TIMEOUT_SECONDS + " seconds). " +
"Requested: " + timeout + " seconds. " +
"Consider using LIMIT clauses in SELECT queries to reduce execution time."
);
}

return ValidationResult.success(timeout);
}

/**
 * Gets the query type (SELECT, ASK, CONSTRUCT, DESCRIBE, or UNKNOWN).
 */
public static String getQueryType(String query) {
if (query == null) return "UNKNOWN";

String upper = query.trim().toUpperCase();
if (upper.startsWith("SELECT")) return "SELECT";
if (upper.startsWith("ASK")) return "ASK";
if (upper.startsWith("CONSTRUCT")) return "CONSTRUCT";
if (upper.startsWith("DESCRIBE")) return "DESCRIBE";
return "UNKNOWN";
}

/**
 * Result of SPARQL query validation.
 */
public static class ValidationResult {
private final boolean valid;
private final String message;
private final int recommendedTimeout;

private ValidationResult(boolean valid, String message, int timeout) {
this.valid = valid;
this.message = message;
this.recommendedTimeout = timeout;
}

public static ValidationResult success(int timeout) {
return new ValidationResult(true, null, timeout);
}

public static ValidationResult error(String message) {
return new ValidationResult(false, message, DEFAULT_TIMEOUT_SECONDS);
}

public boolean isValid() {
return valid;
}

public String getMessage() {
return message;
}

public int getRecommendedTimeout() {
return recommendedTimeout;
}
}
}
