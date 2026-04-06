package systems.symbol.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import systems.symbol.cli.util.LockManager;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command to repair repository issues including stale lock cleanup.
 * 
 * Performs maintenance operations:
 * - Identify and remove stale transaction locks (>1 hour old)
 * - Clean up orphaned lock files
 * - Safe removal (checks file age before deletion)
 * - Reports summary of cleaned locks
 * 
 * Usage:
 *   iq repair locks [--path <path>] [--force] [--dry-run]
 * 
 * Examples:
 *   iq repair locks   # Repair default repository
 *   iq repair locks --path /tmp/repositories/myrepo
 *   iq repair locks --dry-run # Show what would be cleaned
 *   iq repair locks --force   # Force removal without confirmation
 * 
 * Exit codes:
 *   0 = Repair successful
 *   1 = Repair found issues or user declined action
 *   null = Command exception occurred
 * 
 * @author Symbol Systems
 * @since 0.94.1
 */
@Command(
name = "repair",
description = "Repair repository issues (stale locks, etc.)",
mixinStandardHelpOptions = true
)
public class RepairCommand extends AbstractCLICommand implements Callable<Object> {

@Option(
names = {"--path"},
description = "Repository path (default: ${IQ_HOME}/repositories/default)",
defaultValue = "${IQ_HOME}"
)
File repositoryPath;

@Option(
names = {"--operation", "-op"},
description = "Repair operation: 'locks' (default) or 'all'",
defaultValue = "locks"
)
String operation = "locks";

@Option(
names = {"--dry-run"},
description = "Show what would be cleaned without making changes"
)
boolean dryRun = false;

@Option(
names = {"--force", "-f"},
description = "Skip confirmation prompts"
)
boolean force = false;

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
public RepairCommand(CLIContext context) throws java.io.IOException {
super(context);
}

@Override
public Object call() throws Exception {
return execute();
}

/**
 * Execute repair operation.
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

display("Repairing repository: " + repositoryPath.getAbsolutePath());
if (verbose) {
displayf("  Operation: %s%n", operation);
displayf("  Dry-run: %s%n", dryRun);
}

// Step 2: Execute repair operation
boolean success = false;

if ("locks".equalsIgnoreCase(operation) || "all".equalsIgnoreCase(operation)) {
success = repairStaleLocks();
}

if (success) {
display("✓ Repository repair complete");
log.info("Repair successful");
return 0;
} else {
displayError("✗ Repair operation failed or was cancelled");
log.warn("Repair incomplete");
return 1;
}

} catch (Exception e) {
log.error("Repair failed", e);
displayError("✗ Error: " + e.getMessage());
return null;
}
}

/**
 * Repair stale transaction locks in .locks directory.
 */
private boolean repairStaleLocks() {
display("");
display("Checking for stale locks...");

File locksDir = new File(repositoryPath, ".locks");
if (!locksDir.exists()) {
display("  No locks directory found");
return true;
}

File[] locks = locksDir.listFiles((dir, name) -> name.endsWith(".lock"));
if (locks == null || locks.length == 0) {
display("  No lock files found");
return true;
}

display(String.format("  Found %d lock file(s)", locks.length));

// Analyze locks
long now = System.currentTimeMillis();
long STALE_AGE_MS = 3600000L; // 1 hour

int staleCount = 0;
int freshCount = 0;

display("");
display("Lock Status:");
display("-------------");

for (File lockFile : locks) {
long age = now - lockFile.lastModified();
long ageMinutes = age / 60000L;
long ageHours = ageMinutes / 60L;

String status;
if (age > STALE_AGE_MS) {
status = "⚠ STALE";
staleCount++;
} else {
status = "✓ Active";
freshCount++;
}

String ageStr;
if (ageHours > 0) {
ageStr = String.format("%d hours %d minutes", ageHours, ageMinutes % 60);
} else {
ageStr = String.format("%d minutes", ageMinutes);
}

displayf("  %s  %s (%s)%n", status, lockFile.getName(), ageStr);
}

if (staleCount == 0) {
display("");
display("✓ All locks are active (no stale locks found)");
return true;
}

// Ask for confirmation
display("");
displayf("Found %d stale lock file(s) eligible for removal%n", staleCount);

if (dryRun) {
display("DRY RUN: Would remove the above stale locks");
return true;
}

if (!force) {
if (!confirmAction("Remove %d stale lock(s)?", staleCount)) {
display("Operation cancelled by user");
return false;
}
}

// Perform cleanup
display("");
display("Removing stale locks...");

int cleaned = 0;
for (File lockFile : locks) {
long age = now - lockFile.lastModified();
if (age > STALE_AGE_MS) {
try {
if (lockFile.delete()) {
cleaned++;
if (verbose) {
displayf("  ✓ Removed: %s%n", lockFile.getName());
}
log.info("Removed stale lock: {}", lockFile.getName());
} else {
log.warn("Could not delete lock: {}", lockFile);
if (verbose) {
displayf("  ✗ Failed to remove: %s%n", lockFile.getName());
}
}
} catch (Exception e) {
log.warn("Error removing lock", e);
if (verbose) {
displayf("  ✗ Error removing %s: %s%n", lockFile.getName(), e.getMessage());
}
}
}
}

display("");
displayf("✓ Removed %d stale lock(s), %d active lock(s) remain%n", cleaned, freshCount);

return true;
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

if (!repositoryPath.canWrite()) {
return false;
}

return true;
}

/**
 * Ask user to confirm an action via stdin.
 * Returns true if user confirms (y/yes), false otherwise.
 */
private boolean confirmAction(String prompt, Object... args) {
try {
String message = String.format(prompt, args) + " (y/n): ";
display(message);

// In non-interactive mode or with timeout, default to false
System.out.flush();
int response = System.in.read();

return (char) response == 'y' || (char) response == 'Y';
} catch (Exception e) {
log.warn("Error reading confirmation", e);
return false;
}
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
