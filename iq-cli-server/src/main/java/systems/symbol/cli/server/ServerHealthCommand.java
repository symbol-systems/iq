package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.runtime.RuntimeStatus;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks health of the selected runtime")
public class ServerHealthCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
System.out.println("health " + runtimeType + " => " + status);
}
}
