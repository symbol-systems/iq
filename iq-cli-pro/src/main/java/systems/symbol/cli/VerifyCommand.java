package systems.symbol.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import systems.symbol.cli.util.LockManager;
import systems.symbol.cli.util.RDFValidator;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command to verify repository integrity and consistency.
 * 
 * Performs comprehensive checks including:
 * - Repository accessibility and queryability
 * - Namespace consistency
 * - IRI validity (proper URI syntax)
 * - Type consistency
 * - No orphaned triples
 * - Transaction capability
 * 
 * Usage:
 *   iq verify [--path <path>] [--timeout <seconds>] [--verbose]
 * 
 * Examples:
 *   iq verify   # Verify default repository
 *   iq verify --path /tmp/repositories/myrepo
 *   iq verify --timeout 60 --verbose
 * 
 * Exit codes:
 *   0 = Verification successful (no errors)
 *   1 = Verification found errors/warnings
 *   null = Command exception occurred
 * 
 * @author Symbol Systems
 * @since 0.94.1
 */
@Command(
name = "verify",
description = "Verify RDF repository integrity and consistency",
mixinStandardHelpOptions = true
)
public class VerifyCommand extends AbstractCLICommand implements Callable<Object> {

@Option(
names = {"--path"},
description = "Repository path (default: ${IQ_HOME}/repositories/default)",
defaultValue = "${IQ_HOME}"
)
File repositoryPath;

@Option(
names = {"--timeout"},
description = "Lock acquisition timeout in seconds",
defaultValue = "30"
)
int timeout = 30;

@Option(
names = {"--verbose", "-v"},
description = "Enable verbose output"
)
boolean verbose = false;

/**
 * Initialize command with CLI context.
 * 
 * @param context CLI context with workspace and kernel
 * @throws java.io.IOException if unable to initialize context
 */
public VerifyCommand(CLIContext context) throws java.io.IOException {
super(context);
}

@Override
public Object call() throws Exception {
return execute();
}

/**
 * Execute repository verification.
 */
private Object execute() {
try {
// Step 1: Validate prerequisites
if (!context.isInitialized()) {
displayError("✗ IQ workspace not initialized");
log.error("Workspace not initialized");
return 1;
}

if (!validateRepositoryPath()) {
displayError("✗ Repository path is invalid: " + repositoryPath);
log.error("Invalid repository path: {}", repositoryPath);
return 1;
}

display("Verifying repository: " + repositoryPath.getAbsolutePath());
if (verbose) {
displayf("  Timeout: %d seconds%n", timeout);
}

// Step 2: Attempt to acquire lock
LockManager lockMgr = new LockManager(repositoryPath, "verify");
if (!lockMgr.acquire(timeout)) {
displayError("✗ Could not acquire lock (repository in use)");
log.warn("Lock acquisition failed for: {}", repositoryPath);
return 1;
}

if (verbose) {
displayf("  ✓ Lock acquired: %s%n", lockMgr.getLockFile().getAbsolutePath());
}

try {
// Step 3: Get repository connection and run validations
var iq = context.newIQBase();
try {
var conn = iq.getConnection();
try {
// Run comprehensive RDF validation
RDFValidator validator = new RDFValidator(conn);
RDFValidator.ValidationReport report = validator.validate();

// Display results
displayValidationResults(report);

// Determine exit code
if (report.hasErrors()) {
log.warn("Verification found errors");
return 1;
} else {
log.info("Verification successful");
display("✓ Repository verification complete");
return 0;
}

} finally {
conn.close();
}
} finally {
iq.close();
}

} finally {
// Step 4: Release lock
lockMgr.release();
if (verbose) {
displayf("  ✓ Lock released%n");
}
}

} catch (Exception e) {
log.error("Verification failed", e);
displayError("✗ Error: " + e.getMessage());
return null;
}
}

/**
 * Validate repository path exists and is accessible.
 */
private boolean validateRepositoryPath() {
if (repositoryPath == null) {
return false;
}

if (!repositoryPath.exists()) {
return false;
}

if (!repositoryPath.isDirectory()) {
return false;
}

if (!repositoryPath.canRead()) {
return false;
}

return true;
}

/**
 * Display validation report results to user.
 */
private void displayValidationResults(RDFValidator.ValidationReport report) {
display("");
display("Validation Results:");
display("-------------------");

// Count issues by severity
int errors = (int) report.getErrors().stream().count();
int warnings = (int) report.getIssues().stream()
.filter(i -> i.severity == RDFValidator.Severity.WARNING).count();
int infos = (int) report.getIssues().stream()
.filter(i -> i.severity == RDFValidator.Severity.INFO).count();

// Report summary
if (errors > 0) {
displayf("✗ Errors: %d%n", errors);
} else {
displayf("✓ Errors: 0%n");
}

if (warnings > 0) {
displayf("⚠ Warnings: %d%n", warnings);
} else if (!verbose) {
// Only show "0 warnings" if verbose
}

if (verbose && infos > 0) {
displayf("ℹ Info: %d%n", infos);
}

// Detail view (verbose mode)
if (verbose) {
display("");
display("Details:");
display("---------");

for (RDFValidator.ValidationIssue issue : report.getIssues()) {
String icon = switch (issue.severity) {
case ERROR -> "✗";
case WARNING -> "⚠";
case INFO -> "ℹ";
};

displayf("%s [%s] %s%n", icon, issue.severity, issue.title);
displayf("   %s%n", issue.description);
}
}

display("");
display(report.getSummary());
}

/**
 * Display message to user (stdout).
 */
public void display(String message) {
if (context != null) {
context.display(message);
} else {
System.out.println(message);
}
}

/**
 * Display formatted message.
 */
public void displayf(String format, Object... args) {
display(String.format(format, args));
}

/**
 * Display error message (stderr).
 */
public void displayError(String message) {
if (context != null) {
context.displayError(message);
} else {
System.err.println(message);
}
}
}
