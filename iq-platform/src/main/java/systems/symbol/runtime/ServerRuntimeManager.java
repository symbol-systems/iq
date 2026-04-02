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
throw new UnsupportedOperationException("start with port not implemented");
}

default boolean stop(String runtimeType, int port) {
throw new UnsupportedOperationException("stop with port not implemented");
}

default boolean reboot(String runtimeType, int port) {
throw new UnsupportedOperationException("reboot with port not implemented");
}

default RuntimeStatus health(String runtimeType, int port) {
throw new UnsupportedOperationException("health with port not implemented");
}

default boolean debug(String runtimeType, int port, boolean enable) {
throw new UnsupportedOperationException("debug with port not implemented");
}

default ServerDump dump(String runtimeType, int port, String path) {
throw new UnsupportedOperationException("dump with port not implemented");
}

default java.util.Map<String, RuntimeStatus> listRuntimes(String runtimeType) {
return java.util.Collections.emptyMap();
}
}

