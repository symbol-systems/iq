package systems.symbol.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProcessServerRuntimeManager implements ServerRuntimeManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessServerRuntimeManager.class);

    private final ConcurrentMap<String, RuntimeState> runtimes = new ConcurrentHashMap<>();

    public ProcessServerRuntimeManager() {
        runtimes.put("api", new RuntimeState(false));
        runtimes.put("mcp", new RuntimeState(false));
    }

    @Override
    public boolean start(String runtimeType) {
        RuntimeState state = runtimes.computeIfAbsent(runtimeType, rt -> new RuntimeState(false));
        if (state.isRunning()) {
            log.info("Runtime {} is already running", runtimeType);
            return true;
        }
        state.setRunning(true);
        state.setDebug(false);
        state.setDetails("Started at " + System.currentTimeMillis());
        log.info("Started runtime {}", runtimeType);
        return true;
    }

    @Override
    public boolean stop(String runtimeType) {
        RuntimeState state = runtimes.get(runtimeType);
        if (state == null || !state.isRunning()) {
            log.warn("Runtime {} was not running", runtimeType);
            return true;
        }
        state.setRunning(false);
        state.setDebug(false);
        state.setDetails("Stopped at " + System.currentTimeMillis());
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
        RuntimeState state = runtimes.get(runtimeType);
        if (state == null) {
            return new RuntimeStatus(runtimeType, false, "Unknown runtime");
        }
        boolean healthy = state.isRunning();
        return new RuntimeStatus(runtimeType, healthy, state.getDetails());
    }

    @Override
    public boolean debug(String runtimeType, boolean enable) {
        RuntimeState state = runtimes.get(runtimeType);
        if (state == null) {
            state = new RuntimeState(false);
            runtimes.put(runtimeType, state);
        }
        state.setDebug(enable);
        state.setDetails("[" + runtimeType + "] debug " + (enable ? "enabled" : "disabled"));
        log.info("Set debug on runtime {} -> {}", runtimeType, enable);
        return true;
    }

    @Override
    public ServerDump dump(String runtimeType, String path) {
        RuntimeState state = runtimes.get(runtimeType);
        boolean success = state != null && state.isRunning();
        String message = success ? "Dump created to " + path : "Runtime not running";
        log.info("Dump {} for runtime {}: {}", path, runtimeType, message);
        return new ServerDump(runtimeType, path, success, message);
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
