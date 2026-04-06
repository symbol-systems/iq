package systems.symbol.runtime;

public interface ServerRuntimeManager {

default boolean start(String runtimeType) {
return start(runtimeType, 0);
}

default boolean stop(String runtimeType) {
return stop(runtimeType, 0);
}

default boolean reboot(String runtimeType) {
return reboot(runtimeType, 0);
}

default RuntimeStatus health(String runtimeType) {
return health(runtimeType, 0);
}

default boolean debug(String runtimeType, boolean enable) {
return debug(runtimeType, 0, enable);
}

default ServerDump dump(String runtimeType, String path) {
return dump(runtimeType, 0, path);
}

default boolean start(String runtimeType, int port) {
// Port-based operations delegate to the No-arg variant (port=0)
// The implementing class should handle port affinity internally if needed
return start(runtimeType);
}

default boolean stop(String runtimeType, int port) {
return stop(runtimeType);
}

default boolean reboot(String runtimeType, int port) {
return reboot(runtimeType);
}

default RuntimeStatus health(String runtimeType, int port) {
return health(runtimeType);
}

default boolean debug(String runtimeType, int port, boolean enable) {
return debug(runtimeType, enable);
}

default ServerDump dump(String runtimeType, int port, String path) {
return dump(runtimeType, path);
}

default java.util.Map<String, RuntimeStatus> listRuntimes(String runtimeType) {
return java.util.Collections.emptyMap();
}
}

