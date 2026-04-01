package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "stop", description = "Stops the selected runtime")
public class ServerStopCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerStopCommand.class);

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        boolean result = ServerRuntimeManagerFactory.getInstance().stop(runtimeType);
        log.info("stop {} => {}", runtimeType, result ? "OK" : "FAIL");
    }
}
