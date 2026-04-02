package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.RuntimeStatus;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks health of the selected runtime")
public class ServerHealthCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@CommandLine.Option(names = "--port", description = "Port to query", defaultValue = "0")
int port;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType, port);
ConsoleDisplay.getInstance().out(String.format("health %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", status));
}
}
