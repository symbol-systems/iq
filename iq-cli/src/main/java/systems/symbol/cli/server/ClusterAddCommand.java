package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

import java.util.Set;

@CommandLine.Command(name = "add", description = "Add node to cluster")
public class ClusterAddCommand implements Runnable {

@CommandLine.Parameters(index = "0", description = "Node URL")
String node;

@Override
public void run() {
if (!ClusterConfig.isValidNode(node)) {
ConsoleDisplay.getInstance().out("cluster add: invalid node URL: " + node);
return;
}

try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (nodes.contains(node)) {
ConsoleDisplay.getInstance().out("cluster add: node already configured: " + node);
return;
}
nodes.add(node);
ClusterConfig.writeClusterNodes(nodes);
ConsoleDisplay.getInstance().out("cluster add: " + node);
} catch (Exception e) {
ConsoleDisplay.getInstance().out("cluster add: failed to update cluster config: " + e.getMessage());
}
}
}
