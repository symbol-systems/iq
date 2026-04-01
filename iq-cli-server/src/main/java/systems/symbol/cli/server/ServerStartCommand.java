package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "start", description = "Starts the selected runtime")
public class ServerStartCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerStartCommand.class);

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        boolean result = ServerRuntimeManagerFactory.getInstance().start(runtimeType);
        log.info("start {} => {}", runtimeType, result ? "OK" : "FAIL");
    }
}
