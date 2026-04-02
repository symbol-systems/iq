package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "reboot", description = "Reboots the selected runtime")
public class ServerRebootCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@CommandLine.Option(names = "--port", description = "Port of runtime to reboot", defaultValue = "0")
int port;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result;
if (port > 0) {
result = ServerRuntimeManagerFactory.getInstance().reboot(runtimeType);
} else {
result = ServerRuntimeManagerFactory.getInstance().reboot(runtimeType);
}
ConsoleDisplay.getInstance().outf("reboot %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", (result ? "OK" : "FAIL"));
}
}
