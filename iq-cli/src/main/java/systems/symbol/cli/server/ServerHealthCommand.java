package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks runtime health")
public class ServerHealthCommand implements Runnable {

    @CommandLine.Option(names = {"--verbose"}, description = "Show detailed health info")
    boolean verbose;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
        if (verbose) {
            System.out.println("health " + runtimeType + " => " + status);
        } else {
            System.out.println("health " + runtimeType + " => " + (status.isHealthy() ? "UP" : "DOWN"));
        }
    }
}
