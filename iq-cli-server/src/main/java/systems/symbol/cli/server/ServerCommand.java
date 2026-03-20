package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.cli.CLIContext;

@CommandLine.Command(name = "server", description = "Manage IQ runtime server components", subcommands = {
        ApiCommand.class,
        McpCommand.class
})
public class ServerCommand implements Runnable {
    private final CLIContext context;

    public ServerCommand(CLIContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("Use --help for server subcommands: api,mcp");
    }

    public CLIContext getContext() {
        return context;
    }
}
