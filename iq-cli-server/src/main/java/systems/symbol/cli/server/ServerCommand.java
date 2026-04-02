package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.cli.CLIContext;

@CommandLine.Command(name = "server", description = "Manage IQ runtime server components", subcommands = {
ApiCommand.class,
McpCommand.class,
ClusterCommand.class
})
public class ServerCommand implements Runnable {
private final CLIContext context;

public ServerCommand(CLIContext context) {
this.context = context;
}

@Override
public void run() {
ConsoleDisplay.getInstance().out("Use --help for server subcommands: api,mcp");
}

public CLIContext getContext() {
return context;
}
}
