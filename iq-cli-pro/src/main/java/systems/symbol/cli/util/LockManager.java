package systems.symbol.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

/**
 * Utility for managing repository locks via .locks directory pattern.
 * 
 * Provides atomic lock file creation with stale lock detection and cleanup.
 * Locks are stored in .locks/{command}.lock files with metadata (PID, timestamp, hostname).
 * 
 * Example:
 * <pre>
 *   LockManager lock = new LockManager(repositoryPath, "verify");
 *   if (!lock.acquire(30)) {
 *   throw new IOException("Could not acquire lock");
 *   }
 *   try {
 *   // perform RDF operations
 *   } finally {
 *   lock.release();
 *   }
 * </pre>
 * 
 * @author Symbol Systems
 * @since 0.94.3
 */
public class LockManager implements AutoCloseable {

private static final Logger log = LoggerFactory.getLogger(LockManager.class);
private static final String LOCKS_DIR = ".locks";
private static final long STALE_LOCK_AGE_MS = 3600000L; // 1 hour
private static final long BACKOFF_MS = 500L; // 500ms between retries

private final File repositoryPath;
private final String lockName;
private File lockFile;
private boolean acquired = false;

/**
 * Create a new lock manager for a repository.
 * 
 * @param repositoryPath root directory of the RDF repository
 * @param lockName name of the lock (e.g., "verify", "repair")
 */
public LockManager(File repositoryPath, String lockName) {
this.repositoryPath = repositoryPath;
this.lockName = lockName;
}

/**
 * Acquire a lock with exponential backoff and stale lock detection.
 * 
 * @param timeoutSeconds timeout in seconds
 * @return true if lock acquired, false if timeout or error
 * @throws IOException if locks directory cannot be created/accessed
 * @throws InterruptedException if waiting is interrupted
 */
public boolean acquire(int timeoutSeconds) throws IOException, InterruptedException {
File locksDir = new File(repositoryPath, LOCKS_DIR);

// Create locks directory if needed
if (!locksDir.exists() && !locksDir.mkdirs()) {
log.warn("Could not create locks directory: {}", locksDir);
throw new IOException("Cannot create locks directory: " + locksDir);
}

this.lockFile = new File(locksDir, lockName + ".lock");
long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

while (System.currentTimeMillis() < endTime) {
try {
// Attempt atomic lock file creation
if (lockFile.createNewFile()) {
writeLockMetadata();
lockFile.deleteOnExit();
acquired = true;
log.debug("Lock acquired: {}", lockFile.getAbsolutePath());
return true;
}
} catch (IOException e) {
log.debug("Lock creation failed (will retry)", e);
}

// Check if existing lock is stale
if (isLockStale(lockFile)) {
log.info("Detected stale lock (age > 1h), removing: {}", lockFile);
try {
if (lockFile.delete()) {
// Retry immediately
if (lockFile.createNewFile()) {
writeLockMetadata();
lockFile.deleteOnExit();
acquired = true;
log.debug("Lock acquired (recovered stale lock): {}", lockFile);
return true;
}
}
} catch (IOException e) {
log.debug("Stale lock removal failed", e);
}
}

// Exponential backoff: wait before retry
Thread.sleep(BACKOFF_MS);
}

log.warn("Lock timeout after {} seconds: {}", timeoutSeconds, lockFile);
return false;
}

/**
 * Release the lock by deleting the lock file.
 * Safe to call even if lock was not acquired.
 */
public void release() {
if (lockFile != null && lockFile.exists()) {
try {
if (lockFile.delete()) {
acquired = false;
log.debug("Lock released: {}", lockFile);
} else {
log.warn("Could not delete lock file: {}", lockFile);
}
} catch (Exception e) {
log.warn("Error releasing lock", e);
}
}
}

/**
 * Check if lock was successfully acquired.
 */
public boolean isAcquired() {
return acquired;
}

/**
 * Get path to the lock file.
 */
public File getLockFile() {
return lockFile;
}

/**
 * Allow use in try-with-resources for automatic cleanup.
 */
@Override
public void close() {
release();
}

/**
 * Check if a lock file is stale (older than 1 hour).
 */
private boolean isLockStale(File lockFile) {
if (!lockFile.exists()) {
return false;
}

long lastMod = lockFile.lastModified();
long age = System.currentTimeMillis() - lastMod;
return age > STALE_LOCK_AGE_MS;
}

/**
 * Write lock metadata (PID, timestamp, hostname) to lock file.
 */
private void writeLockMetadata() throws IOException {
if (lockFile == null) {
return;
}

try {
StringBuilder metadata = new StringBuilder();
metadata.append("pid=").append(ProcessHandle.current().pid()).append("\n");
metadata.append("time=").append(System.currentTimeMillis()).append("\n");
metadata.append("instant=").append(Instant.now()).append("\n");

try {
String hostname = InetAddress.getLocalHost().getHostName();
metadata.append("host=").append(hostname).append("\n");
} catch (Exception e) {
log.debug("Could not get hostname", e);
metadata.append("host=unknown\n");
}

Files.writeString(
lockFile.toPath(),
metadata.toString(),
StandardCharsets.UTF_8
);
} catch (IOException e) {
log.warn("Could not write lock metadata", e);
}
}

/**
 * Attempt to remove all stale locks in a repository's locks directory.
 * 
 * @param repositoryPath repository root
 * @return number of stale locks removed
 */
public static int cleanupStaleLocks(File repositoryPath) {
File locksDir = new File(repositoryPath, LOCKS_DIR);
if (!locksDir.exists()) {
return 0;
}

int cleaned = 0;
File[] locks = locksDir.listFiles((dir, name) -> name.endsWith(".lock"));

if (locks != null) {
for (File lockFile : locks) {
long age = System.currentTimeMillis() - lockFile.lastModified();
if (age > STALE_LOCK_AGE_MS) {
try {
if (lockFile.delete()) {
cleaned++;
log.info("Removed stale lock: {}", lockFile.getName());
}
} catch (Exception e) {
log.warn("Could not remove stale lock: {}", lockFile, e);
}
}
}
}

return cleaned;
}
}
