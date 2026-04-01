package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "debug", description = "Toggle runtime debug mode")
public class ServerDebugCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerDebugCommand.class);

    @CommandLine.Option(names = {"--enable"}, description = "Enable debug")
    boolean enable;

    @CommandLine.Option(names = {"--disable"}, description = "Disable debug")
    boolean disable;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        boolean action = enable || !disable;
        boolean result = ServerRuntimeManagerFactory.getInstance().debug(runtimeType, action);
        log.info("debug {} -> {} : {}", runtimeType, action ? "enabled" : "disabled", result ? "OK" : "FAIL");
    }
}
