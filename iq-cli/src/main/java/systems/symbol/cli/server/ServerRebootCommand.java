package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "reboot", description = "Reboots the selected runtime")
public class ServerRebootCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerRebootCommand.class);

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        boolean result = ServerRuntimeManagerFactory.getInstance().reboot(runtimeType);
        log.info("reboot {} => {}", runtimeType, result ? "OK" : "FAIL");
    }
}
