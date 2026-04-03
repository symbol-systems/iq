package systems.symbol.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerRuntimeManagerFactory {

private static final Logger log = LoggerFactory.getLogger(ServerRuntimeManagerFactory.class);

private static ServerRuntimeManager instance;

public static synchronized ServerRuntimeManager getInstance() {
if (instance != null) {
return instance;
}

String manager = System.getProperty("iq.runtime.manager");
if (manager == null || manager.isBlank()) {
manager = System.getenv("IQ_RUNTIME_MANAGER");
}
if (manager == null || manager.isBlank()) {
manager = "process";
}

instance = lookupManager(manager.toLowerCase());
log.info("ServerRuntimeManager initialized: {}", instance.getClass().getName());
return instance;
}

private static ServerRuntimeManager lookupManager(String manager) {
switch (manager) {
case "quarkus":
return instantiate("systems.symbol.runtime.QuarkusRuntimeManager");
case "default":
return instantiate("systems.symbol.runtime.DefaultServerRuntimeManager");
case "process":
default:
return instantiate("systems.symbol.runtime.ProcessServerRuntimeManager");
}
}

private static ServerRuntimeManager instantiate(String className) {
try {
Class<?> clazz = Class.forName(className);
Object obj = clazz.getDeclaredConstructor().newInstance();
if (obj instanceof ServerRuntimeManager) {
return (ServerRuntimeManager) obj;
}
log.warn("{} is not instance of ServerRuntimeManager", className);
} catch (ClassNotFoundException e) {
log.warn("Runtime manager implementation not found: {} (fallback to noop)", className);
} catch (Exception e) {
log.error("Failed to instantiate RuntimeManager {}", className, e);
}
return new ServerRuntimeManager() {
// fallback no-op manager
@Override
public boolean start(String runtimeType, int port) { return false; }
@Override
public boolean stop(String runtimeType, int port) { return false; }
@Override
public boolean reboot(String runtimeType, int port) { return false; }
@Override
public RuntimeStatus health(String runtimeType, int port) { return new RuntimeStatus(runtimeType, false, "no manager"); }
@Override
public boolean debug(String runtimeType, int port, boolean enable) { return false; }
@Override
public ServerDump dump(String runtimeType, int port, String path) { return new ServerDump(runtimeType, path, false, "no manager"); }
};
}

private ServerRuntimeManagerFactory() {
}
}
