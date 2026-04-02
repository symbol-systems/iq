package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

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

@Override
public void run() {
ConsoleDisplay.getInstance().out("Use --help for api commands");
}

@Override
public String getRuntimeType() {
return "api";
}
}
