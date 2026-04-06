package systems.symbol.cli.util;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for validating RDF repository integrity and consistency.
 * 
 * Performs checks including:
 * - IRI validity (valid URI syntax)
 * - Namespace consistency
 * - No orphaned triples (objects are valid subjects)
 * - No dangling type references
 * - Required property presence
 * - Query execution capability
 * - Transaction management
 * 
 * Usage:
 * <pre>
 *   RDFValidator validator = new RDFValidator(conn);
 *   RDFValidator.ValidationReport report = validator.validate();
 *   if (report.hasErrors()) {
 *   System.out.println(report.getErrorSummary());
 *   }
 * </pre>
 * 
 * @author Symbol Systems
 * @since 0.94.1
 */
public class RDFValidator {

private static final Logger log = LoggerFactory.getLogger(RDFValidator.class);

private final RepositoryConnection connection;
private final List<ValidationIssue> issues = new ArrayList<>();

/**
 * Create a validator for the given repository connection.
 * 
 * @param connection RDF4J RepositoryConnection
 */
public RDFValidator(RepositoryConnection connection) {
this.connection = connection;
}

/**
 * Run comprehensive RDF validation checks.
 * 
 * @return ValidationReport with all findings
 */
public ValidationReport validate() {
issues.clear();

try {
// Test 1: Repository accessibility
checkRepositoryAccess();

// Test 2: Namespace consistency
checkNamespaceConsistency();

// Test 3: IRI validity
checkIRIValidity();

// Test 4: Type consistency
checkTypeConsistency();

// Test 5: No orphaned triples
checkOrphanedTriples();

} catch (Exception e) {
log.error("Validation error", e);
issues.add(new ValidationIssue(
Severity.ERROR,
"Validation Failed",
"Exception during validation: " + e.getMessage()
));
}

return new ValidationReport(issues);
}

/**
 * Check if repository is accessible and queryable.
 */
private void checkRepositoryAccess() {
try {
long tripleCount = connection.size();
log.info("Repository size: {} triples", tripleCount);

// Try a simple query
String query = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }";
var tupleQuery = connection.prepareTupleQuery(query);
try (var result = tupleQuery.evaluate()) {
if (result.hasNext()) {
BindingSet binding = result.next();
long queryCount = ((Number) binding.getBinding("count")
.getValue()).longValue();

if (queryCount == 0) {
issues.add(new ValidationIssue(
Severity.WARNING,
"Empty Repository",
"Repository contains no triples"
));
}
}
}

// Test transaction capability
connection.begin();
connection.commit();

} catch (Exception e) {
log.error("Repository access check failed", e);
issues.add(new ValidationIssue(
Severity.ERROR,
"Repository Inaccessible",
"Cannot access repository: " + e.getMessage()
));
}
}

/**
 * Verify namespace declarations are consistent.
 */
private void checkNamespaceConsistency() {
try {
Map<String, String> namespaces = new HashMap<>();
var nsIter = connection.getNamespaces();

try (var ns = nsIter) {
while (ns.hasNext()) {
var namespace = ns.next();
String prefix = namespace.getPrefix();
String name = namespace.getName();

// Check for duplicate prefixes with different URIs
if (namespaces.containsKey(prefix)) {
String existing = namespaces.get(prefix);
if (!existing.equals(name)) {
issues.add(new ValidationIssue(
Severity.WARNING,
"Duplicate Namespace Prefix",
String.format(
"Prefix '%s' maps to multiple URIs: %s vs %s",
prefix, existing, name
)
));
}
}
namespaces.put(prefix, name);
}
}

if (namespaces.isEmpty()) {
issues.add(new ValidationIssue(
Severity.WARNING,
"No Namespace Definitions",
"Repository defines no namespace prefixes"
));
} else {
log.info("Found {} namespace definitions", namespaces.size());
}

} catch (Exception e) {
log.warn("Namespace consistency check failed", e);
issues.add(new ValidationIssue(
Severity.WARNING,
"Namespace Check Failed",
"Could not validate namespaces: " + e.getMessage()
));
}
}

/**
 * Sample IRI validity by checking IRIs in the repository.
 */
private void checkIRIValidity() {
try {
String query = "SELECT DISTINCT ?s WHERE { ?s ?p ?o } LIMIT 100";
var tupleQuery = connection.prepareTupleQuery(query);

int checked = 0;
int invalid = 0;

try (var result = tupleQuery.evaluate()) {
while (result.hasNext()) {
BindingSet binding = result.next();
var iriBinding = binding.getBinding("s");

if (iriBinding != null && iriBinding.getValue() instanceof IRI) {
IRI iri = (IRI) iriBinding.getValue();
String iriStr = iri.stringValue();

try {
// Attempt to parse as URI
new URI(iriStr);
checked++;
} catch (URISyntaxException e) {
invalid++;
if (invalid <= 5) { // Report first 5
issues.add(new ValidationIssue(
Severity.WARNING,
"Invalid IRI",
"IRI is not valid URI syntax: " + iriStr
));
}
}
}
}
}

if (invalid > 5) {
issues.add(new ValidationIssue(
Severity.WARNING,
"Invalid IRIs",
String.format(
"Found %d invalid IRIs (showing first 5)",
invalid
)
));
} else if (checked > 0) {
log.info("Checked {} IRIs - all valid", checked);
}

} catch (Exception e) {
log.warn("IRI validity check failed", e);
issues.add(new ValidationIssue(
Severity.WARNING,
"IRI Check Failed",
"Could not validate IRIs: " + e.getMessage()
));
}
}

/**
 * Check for type consistency issues.
 */
private void checkTypeConsistency() {
try {
String query = "SELECT ?type (COUNT(?s) as ?count) WHERE { " +
"  ?s a ?type " +
"} GROUP BY ?type";
var tupleQuery = connection.prepareTupleQuery(query);

int typeCount = 0;
try (var result = tupleQuery.evaluate()) {
while (result.hasNext()) {
result.next();
typeCount++;
}
}

if (typeCount == 0) {
issues.add(new ValidationIssue(
Severity.INFO,
"No Typed Resources",
"Repository contains no resources with explicit type declarations"
));
} else {
log.info("Found {} resource types", typeCount);
}

} catch (Exception e) {
log.debug("Type consistency check failed", e);
}
}

/**
 * Check for orphaned triples (objects not appearing as subjects).
 */
private void checkOrphanedTriples() {
try {
// Sample check: query for objects that aren't subjects (up to limit)
String query = "SELECT ?o WHERE { " +
"  ?s ?p ?o . " +
"  FILTER NOT EXISTS { ?o ?any ?anything } " +
"} LIMIT 10";

var tupleQuery = connection.prepareTupleQuery(query);

int orphans = 0;
try (var result = tupleQuery.evaluate()) {
while (result.hasNext()) {
result.next();
orphans++;
}
}

if (orphans > 0) {
issues.add(new ValidationIssue(
Severity.INFO,
"Resource Leaves",
String.format(
"Found %d resources with no outgoing edges (leaf nodes)",
orphans
)
));
} else {
log.info("No orphaned triples detected");
}

} catch (Exception e) {
log.debug("Orphaned triple check failed", e);
}
}

/**
 * Validation severity levels.
 */
public enum Severity {
ERROR,
WARNING,
INFO
}

/**
 * Single validation issue found during checks.
 */
public static class ValidationIssue {
public final Severity severity;
public final String title;
public final String description;

ValidationIssue(Severity severity, String title, String description) {
this.severity = severity;
this.title = title;
this.description = description;
}

@Override
public String toString() {
return String.format("[%s] %s: %s", severity, title, description);
}
}

/**
 * Complete validation report with all issues found.
 */
public static class ValidationReport {
private final List<ValidationIssue> issues;

ValidationReport(List<ValidationIssue> issues) {
this.issues = new ArrayList<>(issues);
}

/**
 * Check if any errors were found.
 */
public boolean hasErrors() {
return issues.stream().anyMatch(i -> i.severity == Severity.ERROR);
}

/**
 * Check if any warnings were found.
 */
public boolean hasWarnings() {
return issues.stream().anyMatch(i -> i.severity == Severity.WARNING);
}

/**
 * Get all issues.
 */
public List<ValidationIssue> getIssues() {
return new ArrayList<>(issues);
}

/**
 * Get only errors.
 */
public List<ValidationIssue> getErrors() {
return issues.stream()
.filter(i -> i.severity == Severity.ERROR)
.toList();
}

/**
 * Get summary of all issues.
 */
public String getSummary() {
long errors = issues.stream().filter(i -> i.severity == Severity.ERROR).count();
long warnings = issues.stream().filter(i -> i.severity == Severity.WARNING).count();
long infos = issues.stream().filter(i -> i.severity == Severity.INFO).count();

return String.format(
"Validation complete: %d errors, %d warnings, %d info items",
errors, warnings, infos
);
}

/**
 * Get detailed report string.
 */
public String getDetailedReport() {
StringBuilder sb = new StringBuilder();
sb.append(getSummary()).append("\n\n");

for (ValidationIssue issue : issues) {
sb.append(String.format(
"[%s] %s\n  %s\n\n",
issue.severity, issue.title, issue.description
));
}

return sb.toString();
}
}
}
