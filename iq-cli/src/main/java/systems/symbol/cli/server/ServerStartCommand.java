package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "start", description = "Starts the selected runtime")
public class ServerStartCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@CommandLine.Option(names = "--port", description = "Port to bind", defaultValue = "0")
int port;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result = ServerRuntimeManagerFactory.getInstance().start(runtimeType, port);
ConsoleDisplay.getInstance().outf("start %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", (result ? "OK" : "FAIL"));
}
}
