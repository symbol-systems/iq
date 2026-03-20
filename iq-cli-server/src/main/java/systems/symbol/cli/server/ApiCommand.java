package systems.symbol.cli.server;

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
public class ApiCommand implements ServerRuntimeScope {

@Override
public void run() {
System.out.println("Use --help for api commands");
}

@Override
public String getRuntimeType() {
return "api";
}
}
