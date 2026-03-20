package systems.symbol.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class QuarkusRuntimeManager implements ServerRuntimeManager {

private static final Logger log = LoggerFactory.getLogger(QuarkusRuntimeManager.class);
private static final String DEFAULT_HOME = System.getProperty("user.dir");
private static final Path RUNTIME_DIR = Paths.get(System.getProperty("iq.runtime.home", DEFAULT_HOME), ".iq", "runtime");

private final ConcurrentMap<String, ManagedRuntime> runtimes = new ConcurrentHashMap<>();

public QuarkusRuntimeManager() {
try {
Files.createDirectories(RUNTIME_DIR);
} catch (IOException e) {
log.warn("Could not create runtime directory {}", RUNTIME_DIR, e);
}
}

@Override
public boolean start(String runtimeType) {
ManagedRuntime runtime = runtimes.computeIfAbsent(runtimeType, rt -> new ManagedRuntime(rt));
if (runtime.isRunning()) {
log.info("Runtime {} is already running", runtimeType);
return true;
}

String command = resolveCommand(runtimeType);
runtime.setDetails("Starting runtime with: " + command);

try {
ProcessBuilder pb = new ProcessBuilder(parseCommand(command));
pb.directory(Path.of(DEFAULT_HOME).toFile());
pb.redirectOutput(RUNTIME_DIR.resolve(runtimeType + ".out").toFile());
pb.redirectError(RUNTIME_DIR.resolve(runtimeType + ".err").toFile());
Process process = pb.start();
runtime.setProcess(process);
runtime.setRunning(true);
runtime.setDebug(false);
runtime.setDetails("Started runtime " + runtimeType + " (pid=" + process.pid() + ")");
writePidFile(runtimeType, process.pid());
log.info("Started runtime {} with pid {}", runtimeType, process.pid());
return true;
} catch (IOException e) {
runtime.setRunning(false);
runtime.setDetails("Failed to start: " + e.getMessage());
log.error("Failed to start runtime {}", runtimeType, e);
return false;
}
}

@Override
public boolean stop(String runtimeType) {
ManagedRuntime runtime = runtimes.get(runtimeType);
if (runtime == null || !runtime.isRunning()) {
log.warn("Runtime {} is not running", runtimeType);
return true;
}

Process process = runtime.getProcess();
if (process != null && process.isAlive()) {
process.destroy();
try {
process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
if (process.isAlive()) {
process.destroyForcibly();
}
}

runtime.setRunning(false);
runtime.setDebug(false);
runtime.setDetails("Stopped runtime " + runtimeType);
runtimes.remove(runtimeType);
deletePidFile(runtimeType);
log.info("Stopped runtime {}", runtimeType);

return true;
}

@Override
public boolean reboot(String runtimeType) {
log.info("Rebooting runtime {}", runtimeType);
return stop(runtimeType) && start(runtimeType);
}

@Override
public RuntimeStatus health(String runtimeType) {
ManagedRuntime runtime = runtimes.get(runtimeType);
if (runtime == null || !runtime.isRunning()) {
return new RuntimeStatus(runtimeType, false, "not running");
}

Process process = runtime.getProcess();
boolean healthy = process != null && process.isAlive();
String details = runtime.getDetails();

if (healthy) {
details = details + "; process is alive";
} else {
details = details + "; process completed";
}

return new RuntimeStatus(runtimeType, healthy, details);
}

@Override
public boolean debug(String runtimeType, boolean enable) {
ManagedRuntime runtime = runtimes.computeIfAbsent(runtimeType, rt -> new ManagedRuntime(rt));
runtime.setDebug(enable);
runtime.setDetails("Debug mode " + (enable ? "enabled" : "disabled"));
log.info("Set debug {} on runtime {}", enable, runtimeType);
return true;
}

@Override
public ServerDump dump(String runtimeType, String path) {
String outputPath = path == null || path.isBlank() ? RUNTIME_DIR.resolve(runtimeType + ".dump").toString() : path;

RuntimeStatus status = health(runtimeType);
if (!status.isHealthy()) {
return new ServerDump(runtimeType, outputPath, false, "Runtime not healthy: " + status.getDetails());
}

String dumpUrl = System.getProperty("iq.runtime.dump.url", "http://localhost:8080/q/dev/heap-dump");

try {
HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
HttpRequest request = HttpRequest.newBuilder().uri(URI.create(dumpUrl)).GET().build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
Files.writeString(Path.of(outputPath), response.body());
return new ServerDump(runtimeType, outputPath, true,
"Dumped to " + outputPath + " with status " + response.statusCode());
} catch (Exception e) {
log.warn("Unable to create dump for {}: {}", runtimeType, e.getMessage());
return new ServerDump(runtimeType, outputPath, false, "Unable to dump: " + e.getMessage());
}
}

private void writePidFile(String runtimeType, long pid) {
try {
Files.writeString(RUNTIME_DIR.resolve(runtimeType + ".pid"), String.valueOf(pid));
} catch (IOException e) {
log.warn("Unable to write pid file for {}: {}", runtimeType, e.getMessage());
}
}

private void deletePidFile(String runtimeType) {
try {
Files.deleteIfExists(RUNTIME_DIR.resolve(runtimeType + ".pid"));
} catch (IOException e) {
log.warn("Unable to delete pid file for {}: {}", runtimeType, e.getMessage());
}
}

private String resolveCommand(String runtimeType) {
String override = System.getProperty("iq.runtime.command");
if (override != null && !override.isBlank()) {
return override;
}

String envCommand = System.getenv("IQ_QUARKUS_COMMAND");
if (envCommand != null && !envCommand.isBlank()) {
return envCommand;
}

String lower = runtimeType == null ? "" : runtimeType.toLowerCase();
if ("api".equals(lower)) {
return "./mvnw -pl iq-apis quarkus:dev";
} else if ("mcp".equals(lower)) {
return "./mvnw -pl iq-mcp quarkus:dev";
} else {
return "./mvnw -pl iq-apis quarkus:dev";
}
}

private List<String> parseCommand(String command) {
if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
return Arrays.asList("cmd", "/C", command);
}
return Arrays.asList("sh", "-c", command);
}

private static class ManagedRuntime {
private final String name;
private volatile boolean running;
private volatile boolean debug;
private volatile String details;
private volatile Process process;

ManagedRuntime(String name) {
this.name = name;
this.details = "Initialized";
}

boolean isRunning() {
return running;
}

void setRunning(boolean running) {
this.running = running;
}

boolean isDebug() {
return debug;
}

void setDebug(boolean debug) {
this.debug = debug;
}

String getDetails() {
return details;
}

void setDetails(String details) {
this.details = details;
}

Process getProcess() {
return process;
}

void setProcess(Process process) {
this.process = process;
}
}
}
