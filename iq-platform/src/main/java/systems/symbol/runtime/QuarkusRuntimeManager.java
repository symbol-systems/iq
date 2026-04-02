package systems.symbol.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class QuarkusRuntimeManager implements ServerRuntimeManager {

private static final Logger log = LoggerFactory.getLogger(QuarkusRuntimeManager.class);
private static final String DEFAULT_HOME = System.getProperty("user.dir");
private static final Path RUNTIME_DIR = Paths.get(System.getProperty("iq.runtime.home", DEFAULT_HOME), ".iq", "runtime");

private final ConcurrentMap<String, ManagedRuntime> runtimes = new ConcurrentHashMap<>();

private String key(String runtimeType, int port) {
return port > 0 ? runtimeType + ":" + port : runtimeType + ":0";
}

private String encodeId(String id) {
return id.replace(':', '_');
}

private String decodeId(String id) {
return id.replace('_', ':');
}

private Path pidPath(String id) {
return RUNTIME_DIR.resolve(encodeId(id) + ".pid");
}

public QuarkusRuntimeManager() {
try {
Files.createDirectories(RUNTIME_DIR);
loadExistingRuntimes();
} catch (IOException e) {
log.warn("Could not create runtime directory {}", RUNTIME_DIR, e);
}
}

private void loadExistingRuntimes() {
try (DirectoryStream<Path> stream = Files.newDirectoryStream(RUNTIME_DIR, "*.pid")) {
for (Path file : stream) {
String encoded = file.getFileName().toString().replaceFirst("\\.pid$", "");
String id = decodeId(encoded);
ManagedRuntime runtime = constructFromPidFile(id);
if (runtime != null) {
runtimes.put(id, runtime);
}
}
} catch (IOException e) {
log.debug("Unable to read existing runtimes", e);
}
}

private boolean isProcessAlive(long pid) {
return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
}

private ManagedRuntime constructFromPidFile(String id) {
Path path = pidPath(id);
if (!Files.exists(path)) {
return null;
}
try {
long pid = Long.parseLong(Files.readString(path).trim());
boolean running = isProcessAlive(pid);
ManagedRuntime runtime = new ManagedRuntime(id);
runtime.setRunning(running);
runtime.setDetails(running ? "Recovered running pid " + pid : "Recovered stopped pid " + pid);
runtime.setProcess(null); // actual Process object is not available across JVM restarts
if (!running) {
deletePidFile(id);
}
return runtime;
} catch (Exception ex) {
log.warn("Malformed pid file for {}: {}", id, ex.getMessage());
deletePidFile(id);
return null;
}
}

private boolean reclaimOrDetectRunning(String id) {
ManagedRuntime runtime = runtimes.get(id);
if (runtime != null && runtime.isRunning()) {
return true;
}
ManagedRuntime recovered = constructFromPidFile(id);
if (recovered != null && recovered.isRunning()) {
runtimes.put(id, recovered);
return true;
}
return false;
}

@Override
public boolean start(String runtimeType) {
return start(runtimeType, 0);
}

@Override
public boolean start(String runtimeType, int port) {
String id = key(runtimeType, port);
if (reclaimOrDetectRunning(id)) {
log.info("Runtime {} is already running", id);
return true;
}

String command = resolveCommand(runtimeType, port);
ManagedRuntime runtime = new ManagedRuntime(id);
runtime.setDetails("Starting runtime with: " + command);

try {
ProcessBuilder pb = new ProcessBuilder(parseCommand(command));
pb.directory(Path.of(DEFAULT_HOME).toFile());
pb.redirectOutput(RUNTIME_DIR.resolve(encodeId(id) + ".out").toFile());
pb.redirectError(RUNTIME_DIR.resolve(encodeId(id) + ".err").toFile());
Process process = pb.start();
runtime.setProcess(process);
runtime.setRunning(true);
runtime.setDetails("Started runtime " + id + " (pid=" + process.pid() + ")");
runtimes.put(id, runtime);
writePidFile(id, process.pid());
log.info("Started runtime {} with pid {}", id, process.pid());
return true;
} catch (IOException e) {
runtime.setRunning(false);
runtime.setDetails("Failed to start: " + e.getMessage());
log.error("Failed to start runtime {}", id, e);
return false;
}
}

@Override
public boolean stop(String runtimeType) {
return stop(runtimeType, 0);
}

@Override
public boolean stop(String runtimeType, int port) {
String id = key(runtimeType, port);
ManagedRuntime runtime = runtimes.computeIfAbsent(id, rt -> constructFromPidFile(rt));

if (runtime == null || !runtime.isRunning()) {
log.warn("Runtime {} is not running", id);
deletePidFile(id);
runtimes.remove(id);
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
runtime.setDetails("Stopped runtime " + id);
runtimes.remove(id);
deletePidFile(id);
log.info("Stopped runtime {}", id);

return true;
}

@Override
public boolean reboot(String runtimeType) {
return reboot(runtimeType, 0);
}

@Override
public boolean reboot(String runtimeType, int port) {
String id = key(runtimeType, port);
log.info("Rebooting runtime {}", id);
return stop(runtimeType, port) && start(runtimeType, port);
}

@Override
public RuntimeStatus health(String runtimeType) {
return health(runtimeType, 0);
}

@Override
public RuntimeStatus health(String runtimeType, int port) {
String id = key(runtimeType, port);
ManagedRuntime runtime = runtimes.computeIfAbsent(id, rt -> constructFromPidFile(rt));

if (runtime == null || !runtime.isRunning()) {
return new RuntimeStatus(id, false, "not running");
}

Process process = runtime.getProcess();
boolean healthy = process != null && process.isAlive();
String details = runtime.getDetails();

if (healthy) {
details = details + "; process is alive";
} else {
details = details + "; process completed";
}

return new RuntimeStatus(id, healthy, details);
}

@Override
public boolean debug(String runtimeType, boolean enable) {
return debug(runtimeType, 0, enable);
}

@Override
public boolean debug(String runtimeType, int port, boolean enable) {
String id = key(runtimeType, port);
ManagedRuntime runtime = runtimes.computeIfAbsent(id, rt -> constructFromPidFile(rt));
if (runtime == null) {
runtime = new ManagedRuntime(id);
}
runtime.setDetails("Debug mode " + (enable ? "enabled" : "disabled"));
log.info("Set debug {} on runtime {}", enable, id);
runtimes.put(id, runtime);
return true;
}

@Override
public ServerDump dump(String runtimeType, String path) {
return dump(runtimeType, 0, path);
}

@Override
public ServerDump dump(String runtimeType, int port, String path) {
String id = key(runtimeType, port);
String outputPath = path == null || path.isBlank() ? RUNTIME_DIR.resolve(encodeId(id) + ".dump").toString() : path;

RuntimeStatus status = health(runtimeType, port);
if (!status.isHealthy()) {
return new ServerDump(id, outputPath, false, "Runtime not healthy: " + status.getDetails());
}

String dumpUrl = System.getProperty("iq.runtime.dump.url");
if (dumpUrl == null || dumpUrl.isBlank()) {
dumpUrl = System.getenv("IQ_RUNTIME_DUMP_URL");
}
if (dumpUrl == null || dumpUrl.isBlank()) {
dumpUrl = "http://localhost:8080/q/dev/heap-dump";
log.debug("Using default dump URL (configure via IQ_RUNTIME_DUMP_URL or iq.runtime.dump.url): {}", dumpUrl);
}

try {
HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
HttpRequest request = HttpRequest.newBuilder().uri(URI.create(dumpUrl)).GET().build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
Files.writeString(Path.of(outputPath), response.body());
return new ServerDump(id, outputPath, true,
"Dumped to " + outputPath + " with status " + response.statusCode());
} catch (Exception e) {
log.warn("Unable to create dump for {}: {}", id, e.getMessage());
return new ServerDump(id, outputPath, false, "Unable to dump: " + e.getMessage());
}
}

@Override
public Map<String, RuntimeStatus> listRuntimes(String runtimeType) {
Map<String, RuntimeStatus> out = new HashMap<>();
String prefix = runtimeType + ":";
try (DirectoryStream<Path> stream = Files.newDirectoryStream(RUNTIME_DIR, "*.pid")) {
for (Path path : stream) {
String encoded = path.getFileName().toString().replaceFirst("\\.pid$", "");
String id = decodeId(encoded);
if (id.equals(runtimeType + ":0") || id.startsWith(prefix)) {
int port = Integer.parseInt(id.substring(id.indexOf(":") + 1));
RuntimeStatus rs = health(runtimeType, port);
out.put(id, rs);
}
}
} catch (IOException e) {
log.warn("Unable to list runtime pids", e);
}
return out;
}

private void writePidFile(String id, long pid) {
try {
Files.writeString(pidPath(id), String.valueOf(pid));
} catch (IOException e) {
log.warn("Unable to write pid file for {}: {}", id, e.getMessage());
}
}

private void deletePidFile(String id) {
try {
Files.deleteIfExists(pidPath(id));
} catch (IOException e) {
log.warn("Unable to delete pid file for {}: {}", id, e.getMessage());
}
}

private String resolveCommand(String runtimeType, int port) {
String override = System.getProperty("iq.runtime.command");
if (override != null && !override.isBlank()) {
return override;
}

String envCommand = System.getenv("IQ_QUARKUS_COMMAND");
if (envCommand != null && !envCommand.isBlank()) {
return envCommand;
}

String lower = runtimeType == null ? "" : runtimeType.toLowerCase();
String base;
if ("api".equals(lower)) {
base = "./mvnw -pl iq-apis quarkus:dev";
} else if ("mcp".equals(lower)) {
base = "./mvnw -pl iq-mcp quarkus:dev";
} else {
base = "./mvnw -pl iq-apis quarkus:dev";
}

if (port > 0) {
base += " -Dquarkus.http.port=" + port;
}
return base;
}

private List<String> parseCommand(String command) {
if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
return Arrays.asList("cmd", "/C", command);
}
return Arrays.asList("sh", "-c", command);
}

private static class ManagedRuntime {
private volatile boolean running;
private volatile boolean debug;
private volatile String details;
private volatile Process process;

ManagedRuntime() {
this.details = "Initialized";
}

ManagedRuntime(String runtimeType) {
this.details = "Initialized runtime " + runtimeType;
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
