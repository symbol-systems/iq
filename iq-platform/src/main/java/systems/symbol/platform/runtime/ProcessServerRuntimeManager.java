package systems.symbol.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProcessServerRuntimeManager implements ServerRuntimeManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessServerRuntimeManager.class);
    private static final String DEFAULT_HOME = System.getProperty("user.dir");
    private static final Path RUNTIME_DIR = Paths.get(System.getProperty("iq.runtime.home", DEFAULT_HOME), ".iq", "runtime");

    private final ConcurrentMap<String, RuntimeState> runtimes = new ConcurrentHashMap<>();

    public ProcessServerRuntimeManager() {
        try {
            Files.createDirectories(RUNTIME_DIR);
        } catch (IOException e) {
            log.warn("Could not create runtime directory {}", RUNTIME_DIR, e);
        }
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

        String command = resolveCommand(runtimeType);
        state.setDetails("Starting with: " + command);

        try {
            ProcessBuilder pb = new ProcessBuilder(parseCommand(command));
            pb.directory(Path.of(DEFAULT_HOME).toFile());
            pb.redirectOutput(RUNTIME_DIR.resolve(runtimeType + ".out").toFile());
            pb.redirectError(RUNTIME_DIR.resolve(runtimeType + ".err").toFile());
            
            Process process = pb.start();
            state.setProcess(process);
            state.setRunning(true);
            state.setDebug(false);
            state.setDetails("Started (pid=" + process.pid() + ") at " + System.currentTimeMillis());
            writePidFile(runtimeType, process.pid());
            
            log.info("Started runtime {} with pid {}", runtimeType, process.pid());
            return true;
        } catch (IOException e) {
            state.setRunning(false);
            state.setDetails("Failed to start: " + e.getMessage());
            log.error("Failed to start runtime {}", runtimeType, e);
            return false;
        }
    }

    @Override
    public boolean stop(String runtimeType) {
        RuntimeState state = runtimes.get(runtimeType);
        if (state == null || !state.isRunning()) {
            log.warn("Runtime {} was not running", runtimeType);
            return true;
        }

        Process process = state.getProcess();
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

        state.setRunning(false);
        state.setDebug(false);
        state.setDetails("Stopped at " + System.currentTimeMillis());
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
        RuntimeState state = runtimes.get(runtimeType);
        if (state == null) {
            return new RuntimeStatus(runtimeType, false, "Unknown runtime");
        }
        
        Process process = state.getProcess();
        boolean healthy = state.isRunning() && (process == null || process.isAlive());
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
        if (state == null || !state.isRunning()) {
            String message = "Runtime " + runtimeType + " not running — cannot create dump";
            log.warn(message);
            return new ServerDump(runtimeType, path, false, message);
        }

        Path dumpPath = Paths.get(path);
        try {
            // Ensure parent directory exists
            Path parentDir = dumpPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Collect runtime info into dump file
            StringBuilder dumpContent = new StringBuilder();
            dumpContent.append("# IQ Runtime Dump\n");
            dumpContent.append("runtime: ").append(runtimeType).append("\n");
            dumpContent.append("timestamp: ").append(java.time.Instant.now()).append("\n");
            dumpContent.append("running: ").append(state.isRunning()).append("\n");
            dumpContent.append("debug: ").append(state.isDebug()).append("\n");
            dumpContent.append("details: ").append(state.getDetails()).append("\n");
            Process process = state.getProcess();
            if (process != null) {
                dumpContent.append("pid: ").append(process.pid()).append("\n");
                dumpContent.append("alive: ").append(process.isAlive()).append("\n");
            }

            Files.writeString(dumpPath, dumpContent.toString());

            // Verify the file was actually written
            if (!Files.exists(dumpPath) || Files.size(dumpPath) == 0) {
                String message = "Dump file was not created or is empty: " + path;
                log.error(message);
                return new ServerDump(runtimeType, path, false, message);
            }

            log.info("Dump created for runtime {} at {} ({} bytes)", runtimeType, path, Files.size(dumpPath));
            return new ServerDump(runtimeType, path, true, "Dump created (" + Files.size(dumpPath) + " bytes)");
        } catch (IOException e) {
            String message = "Failed to create dump file: " + e.getMessage();
            log.error(message, e);
            return new ServerDump(runtimeType, path, false, message);
        }
    }

    private String resolveCommand(String runtimeType) {
        // Allow override via system property or environment variable
        String override = System.getProperty("iq.runtime.command." + runtimeType);
        if (override != null && !override.isBlank()) {
            return override;
        }

        String envCommand = System.getenv("IQ_RUNTIME_COMMAND_" + runtimeType.toUpperCase());
        if (envCommand != null && !envCommand.isBlank()) {
            return envCommand;
        }

        String lower = runtimeType == null ? "" : runtimeType.toLowerCase();
        if ("api".equals(lower)) {
            return "./mvnw -pl iq-apis quarkus:dev";
        } else if ("mcp".equals(lower)) {
            return "java -jar iq-mcp/target/iq-mcp.jar";
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

    private static class RuntimeState {
        private volatile boolean running;
        private volatile boolean debug;
        private volatile String details;
        private volatile Process process;

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

        public Process getProcess() {
            return process;
        }

        public void setProcess(Process process) {
            this.process = process;
        }
    }
}
