package systems.symbol.platform.runtime;

public interface ServerRuntimeManager {

default boolean start(String runtimeType) {
throw new UnsupportedOperationException("start not implemented");
}

default boolean stop(String runtimeType) {
throw new UnsupportedOperationException("stop not implemented");
}

default boolean reboot(String runtimeType) {
throw new UnsupportedOperationException("reboot not implemented");
}

default RuntimeStatus health(String runtimeType) {
throw new UnsupportedOperationException("health not implemented");
}

default boolean debug(String runtimeType, boolean enable) {
throw new UnsupportedOperationException("debug not implemented");
}

default ServerDump dump(String runtimeType, String path) {
throw new UnsupportedOperationException("dump not implemented");
}
}
