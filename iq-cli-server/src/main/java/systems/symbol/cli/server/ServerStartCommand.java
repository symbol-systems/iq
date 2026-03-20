package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "start", description = "Starts the selected runtime")
public class ServerStartCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result = ServerRuntimeManagerFactory.getInstance().start(runtimeType);
System.out.println("start " + runtimeType + " => " + (result ? "OK" : "FAIL"));
}
}
