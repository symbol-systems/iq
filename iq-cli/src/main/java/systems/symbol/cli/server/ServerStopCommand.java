package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "stop", description = "Stops the selected runtime")
public class ServerStopCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@CommandLine.Option(names = "--port", description = "Port of runtime to stop", defaultValue = "0")
int port;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result;
if (port > 0) {
result = ServerRuntimeManagerFactory.getInstance().stop(runtimeType);
} else {
result = ServerRuntimeManagerFactory.getInstance().stop(runtimeType);
}
ConsoleDisplay.getInstance().outf("stop %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", (result ? "OK" : "FAIL"));
}
}
