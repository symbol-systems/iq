package systems.symbol.cli.server;

import picocli.CommandLine;

@CommandLine.Command(name = "add", description = "Add node to cluster")
public class ClusterAddCommand implements Runnable {

@CommandLine.Parameters(index = "0", description = "Node ID/path")
String node;

@Override
public void run() {
System.out.println("cluster add: " + node);
}
}
