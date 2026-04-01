package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks runtime health")
public class ServerHealthCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerHealthCommand.class);

    @CommandLine.Option(names = {"--verbose"}, description = "Show detailed health info")
    boolean verbose;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
        if (verbose) {
            log.info("health {} => {}", runtimeType, status);
        } else {
            log.info("health {} => {}", runtimeType, status.isHealthy() ? "UP" : "DOWN");
        }
    }
}
