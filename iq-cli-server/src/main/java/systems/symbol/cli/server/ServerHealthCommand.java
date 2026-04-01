package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks health of the selected runtime")
public class ServerHealthCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerHealthCommand.class);

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
        log.info("health {} => {}", runtimeType, status);
    }
}
