package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.cli.CLIContext;

@CommandLine.Command(name = "server", description = "Manage IQ runtime server components", subcommands = {
        ApiCommand.class,
        McpCommand.class,
        ClusterCommand.class
})
public class ServerCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerCommand.class);
    private final CLIContext context;

    public ServerCommand(CLIContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        log.info("Use --help for server subcommands: api,mcp");
    }

    public CLIContext getContext() {
        return context;
    }
}
