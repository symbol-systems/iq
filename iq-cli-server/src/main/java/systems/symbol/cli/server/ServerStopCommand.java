package systems.symbol.cli.server;package systems.symbol.cli.server;


















}}System.out.println("stop " + runtimeType + " => " + (result ? "OK" : "FAIL"));boolean result = ServerRuntimeManagerFactory.getInstance().stop(runtimeType);String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";public void run() {@OverrideServerRuntimeScope parent;@CommandLine.ParentCommandpublic class ServerStopCommand implements Runnable {@CommandLine.Command(name = "stop", description = "Stops the selected runtime")import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;import picocli.CommandLine;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

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
