package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "debug", description = "Toggle runtime debug mode")
public class ServerDebugCommand implements Runnable {

@CommandLine.Option(names = {"--enable"}, description = "Enable debug")
boolean enable;

@CommandLine.Option(names = {"--disable"}, description = "Disable debug")
boolean disable;

@CommandLine.Option(names = "--port", description = "Port to apply", defaultValue = "0")
int port;

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean action = enable || !disable;
boolean result = ServerRuntimeManagerFactory.getInstance().debug(runtimeType, port, action);
ConsoleDisplay.getInstance().out(String.format("debug %s%s -> %s : %s", runtimeType, port > 0 ? ":" + port : "", (action ? "enabled" : "disabled"), (result ? "OK" : "FAIL")));
}
}
