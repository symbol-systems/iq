package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.cli.CLIContext;

@CommandLine.Command(name = "server", description = "Manage IQ runtime server components", subcommands = {
ApiCommand.class,
McpCommand.class,
ClusterCommand.class
})
public class ServerCommand implements Runnable {

private final CLIContext context;

@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show help message")
private boolean helpRequested;

public ServerCommand() {
this(null);
}

public ServerCommand(CLIContext context) {
this.context = context;
}

@Override
public void run() {
if (context != null) {
context.display("Use --help for server subcommands: api,mcp,cluster");
}
}

public CLIContext getContext() {
return context;
}
}
