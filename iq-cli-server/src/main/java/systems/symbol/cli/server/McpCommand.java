package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

@CommandLine.Command(name = "mcp", description = "Manage MCP runtime", subcommands = {
ServerStartCommand.class,
ServerStopCommand.class,
ServerRebootCommand.class,
ServerStatusCommand.class,
ServerHealthCommand.class,
ServerDebugCommand.class,
ServerDumpCommand.class
})
public class McpCommand implements ServerRuntimeScope {

public void run() {
ConsoleDisplay.getInstance().out("Use --help for mcp commands");
}

@Override
public String getRuntimeType() {
return "mcp";
}
}
