package systems.symbol.cli.server;

import picocli.CommandLine;

@CommandLine.Command(name = "remove", description = "Remove node from cluster")
public class ClusterRemoveCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Node ID/path")
    String node;

    @Override
    public void run() {
        System.out.println("cluster remove: " + node);
    }
}
