package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.RuntimeStatus;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "status", description = "Displays runtime status")
public class ServerStatusCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@CommandLine.Option(names = "--port", description = "Port to query", defaultValue = "0")
int port;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
if (port > 0) {
RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType, port);
ConsoleDisplay.getInstance().out(String.format("status %s:%d => %s%n", runtimeType, port, status));
} else {
var all = ServerRuntimeManagerFactory.getInstance().listRuntimes(runtimeType);
if (all.isEmpty()) {
ConsoleDisplay.getInstance().out(String.format("status %s => no active instances%n", runtimeType));
} else {
all.forEach((key, runtimeStatus) -> ConsoleDisplay.getInstance().out(String.format("status %s => %s%n", key, runtimeStatus)));
}
}
}
}
