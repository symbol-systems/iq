package systems.symbol.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contract for managing IQ runtime lifecycle (start, stop, health, dump, etc.).
 *
 * <p>Default implementations log a warning and return a safe failure value;
 * concrete implementations must override all methods they support.</p>
 */
public interface ServerRuntimeManager {

    Logger log = LoggerFactory.getLogger(ServerRuntimeManager.class);

    default boolean start(String runtimeType) {
        log.warn("[{}] start({}) not implemented", getClass().getSimpleName(), runtimeType);
        return false;
    }

    default boolean stop(String runtimeType) {
        log.warn("[{}] stop({}) not implemented", getClass().getSimpleName(), runtimeType);
        return false;
    }

    default boolean reboot(String runtimeType) {
        log.warn("[{}] reboot({}) not implemented", getClass().getSimpleName(), runtimeType);
        return false;
    }

    default RuntimeStatus health(String runtimeType) {
        log.warn("[{}] health({}) not implemented", getClass().getSimpleName(), runtimeType);
        return new RuntimeStatus(runtimeType, false, "Not implemented");
    }

    default boolean debug(String runtimeType, boolean enable) {
        log.warn("[{}] debug({}, {}) not implemented", getClass().getSimpleName(), runtimeType, enable);
        return false;
    }

    default ServerDump dump(String runtimeType, String path) {
        log.warn("[{}] dump({}) not implemented", getClass().getSimpleName(), runtimeType);
        return new ServerDump(runtimeType, path, false, "Not implemented");
    }
}
