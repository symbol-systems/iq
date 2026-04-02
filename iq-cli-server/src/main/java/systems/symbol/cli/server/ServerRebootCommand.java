package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "reboot", description = "Reboots the selected runtime")
public class ServerRebootCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result = ServerRuntimeManagerFactory.getInstance().reboot(runtimeType);
ConsoleDisplay.getInstance().out("reboot " + runtimeType + " => " + (result ? "OK" : "FAIL"));
}
}
