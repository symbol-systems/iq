package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "status", description = "Displays runtime status")
public class ServerStatusCommand implements Runnable {

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
        System.out.println("status " + runtimeType + " => " + status);
    }
}
