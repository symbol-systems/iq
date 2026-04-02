package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "debug", description = "Toggle runtime debug mode")
public class ServerDebugCommand implements Runnable {

@CommandLine.Option(names = {"--enable"}, description = "Enable debug")
boolean enable;

@CommandLine.Option(names = {"--disable"}, description = "Disable debug")
boolean disable;

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean action = enable || !disable;
boolean result = ServerRuntimeManagerFactory.getInstance().debug(runtimeType, action);
System.out.println("debug " + runtimeType + " -> " + (action ? "enabled" : "disabled") + " : " + (result ? "OK" : "FAIL"));
}
}
