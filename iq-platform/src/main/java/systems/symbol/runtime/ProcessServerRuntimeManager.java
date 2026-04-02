package systems.symbol.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProcessServerRuntimeManager implements ServerRuntimeManager {

private static final Logger log = LoggerFactory.getLogger(ProcessServerRuntimeManager.class);

private final ConcurrentMap<String, RuntimeState> runtimes = new ConcurrentHashMap<>();

public ProcessServerRuntimeManager() {
runtimes.put(key("api", 0), new RuntimeState(false));
runtimes.put(key("mcp", 0), new RuntimeState(false));
}

private String key(String runtimeType, int port) {
return port > 0 ? runtimeType + ":" + port : runtimeType + ":0";
}

@Override
public boolean start(String runtimeType) {
return start(runtimeType, 0);
}

@Override
public boolean start(String runtimeType, int port) {
String id = key(runtimeType, port);
RuntimeState state = runtimes.computeIfAbsent(id, rt -> new RuntimeState(false));
if (state.isRunning()) {
log.info("Runtime {} is already running", id);
return true;
}
state.setRunning(true);
state.setDebug(false);
state.setDetails("Started at " + System.currentTimeMillis());
log.info("Started runtime {}", id);
return true;
}

@Override
public boolean stop(String runtimeType) {
return stop(runtimeType, 0);
}

@Override
public boolean stop(String runtimeType, int port) {
String id = key(runtimeType, port);
RuntimeState state = runtimes.get(id);
if (state == null || !state.isRunning()) {
log.warn("Runtime {} was not running", id);
return true;
}
state.setRunning(false);
state.setDebug(false);
state.setDetails("Stopped at " + System.currentTimeMillis());
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
RuntimeState state = runtimes.get(id);
if (state == null) {
return new RuntimeStatus(id, false, "Unknown runtime");
}
boolean healthy = state.isRunning();
return new RuntimeStatus(id, healthy, state.getDetails());
}

@Override
public boolean debug(String runtimeType, boolean enable) {
return debug(runtimeType, 0, enable);
}

@Override
public boolean debug(String runtimeType, int port, boolean enable) {
String id = key(runtimeType, port);
RuntimeState state = runtimes.computeIfAbsent(id, rt -> new RuntimeState(false));
state.setDebug(enable);
state.setDetails("[" + id + "] debug " + (enable ? "enabled" : "disabled"));
log.info("Set debug on runtime {} -> {}", id, enable);
return true;
}

@Override
public ServerDump dump(String runtimeType, String path) {
return dump(runtimeType, 0, path);
}

@Override
public ServerDump dump(String runtimeType, int port, String path) {
String id = key(runtimeType, port);
RuntimeState state = runtimes.get(id);
boolean success = state != null && state.isRunning();
String message = success ? "Dump created to " + path : "Runtime not running";
log.info("Dump {} for runtime {}: {}", path, id, message);
return new ServerDump(id, path, success, message);
}

@Override
public java.util.Map<String, RuntimeStatus> listRuntimes(String runtimeType) {
java.util.Map<String, RuntimeStatus> out = new java.util.HashMap<>();
String prefix = runtimeType + ":";
runtimes.forEach((key, value) -> {
if (key.startsWith(prefix)) {
out.put(key, new RuntimeStatus(key, value.isRunning(), value.getDetails()));
}
});
return out;
}

private static class RuntimeState {
private volatile boolean running;
private volatile boolean debug;
private volatile String details;

RuntimeState(boolean running) {
this.running = running;
this.details = "Initialized";
}

public boolean isRunning() {
return running;
}

public void setRunning(boolean running) {
this.running = running;
}

public boolean isDebug() {
return debug;
}

public void setDebug(boolean debug) {
this.debug = debug;
}

public String getDetails() {
return details;
}

public void setDetails(String details) {
this.details = details;
}
}
}
