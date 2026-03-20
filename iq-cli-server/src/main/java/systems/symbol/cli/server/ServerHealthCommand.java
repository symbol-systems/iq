package systems.symbol.cli.server;package systems.symbol.cli.server;


























}    }        }            System.out.println("health " + runtimeType + " => " + (status.isHealthy() ? "UP" : "DOWN"));        } else {            System.out.println("health " + runtimeType + " => " + status);        if (verbose) {        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";    public void run() {    @Override    boolean verbose;    @CommandLine.Option(names = {"--verbose"}, description = "Show detailed health info")    ServerRuntimeScope parent;    @CommandLine.ParentCommandpublic class ServerHealthCommand implements Runnable {@CommandLine.Command(name = "health", description = "Checks runtime health")import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;import systems.symbol.platform.runtime.RuntimeStatus;import picocli.CommandLine;
import picocli.CommandLine;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "health", description = "Checks runtime health")
public class ServerHealthCommand implements Runnable {

    @CommandLine.Option(names = {"--verbose", "-v"}, description = "Verbose output")
    boolean verbose = false;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(runtimeType);
        if (verbose) {
            System.out.println(status.toString());
        } else {
            System.out.println("health " + runtimeType + " => " + (status.isHealthy() ? "OK" : "FAIL"));
        }
    }
}
