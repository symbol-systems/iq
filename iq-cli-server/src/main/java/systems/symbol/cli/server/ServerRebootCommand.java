package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "reboot", description = "Reboots the selected runtime")
public class ServerRebootCommand implements Runnable {

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
boolean result = ServerRuntimeManagerFactory.getInstance().reboot(runtimeType);
System.out.println("reboot " + runtimeType + " => " + (result ? "OK" : "FAIL"));
}
}
