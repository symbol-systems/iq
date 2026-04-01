package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "mcp", description = "Manage MCP runtime", subcommands = {
        ServerStartCommand.class,
        ServerStopCommand.class,
        ServerRebootCommand.class,
        ServerStatusCommand.class,
        ServerHealthCommand.class,
        ServerDebugCommand.class,
        ServerDumpCommand.class
})
public class McpCommand implements ServerRuntimeScope, Runnable {
    private static final Logger log = LoggerFactory.getLogger(McpCommand.class);

    @Override
    public void run() {
        log.info("Use --help for mcp commands");
    }

    @Override
    public String getRuntimeType() {
        return "mcp";
    }
}
