package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "stop", description = "Stops the selected runtime")
public class ServerStopCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result = ServerRuntimeManagerFactory.getInstance().stop(runtimeType);
System.out.println("stop " + runtimeType + " => " + (result ? "OK" : "FAIL"));
}
}
