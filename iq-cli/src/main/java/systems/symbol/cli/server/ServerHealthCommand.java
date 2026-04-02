package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.RuntimeStatus;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks runtime health")
public class ServerHealthCommand implements Runnable {

@CommandLine.Option(names = {"--verbose"}, description = "Show detailed health info")
boolean verbose;

@CommandLine.Option(names = "--port", description = "Port to query", defaultValue = "0")
int port;

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType, port);
if (verbose) {
ConsoleDisplay.getInstance().outf("health %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", status);
} else {
ConsoleDisplay.getInstance().outf("health %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", status.isHealthy() ? "UP" : "DOWN");
}
}
}
