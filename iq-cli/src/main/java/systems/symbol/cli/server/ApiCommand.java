package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "api", description = "Manage API runtime", subcommands = {
        ServerStartCommand.class,
        ServerStopCommand.class,
        ServerRebootCommand.class,
        ServerStatusCommand.class,
        ServerHealthCommand.class,
        ServerDebugCommand.class,
        ServerDumpCommand.class
})
public class ApiCommand implements ServerRuntimeScope, Runnable {
    private static final Logger log = LoggerFactory.getLogger(ApiCommand.class);

    @Override
    public void run() {
        log.info("Use --help for api commands");
    }

    @Override
    public String getRuntimeType() {
        return "api";
    }
}
