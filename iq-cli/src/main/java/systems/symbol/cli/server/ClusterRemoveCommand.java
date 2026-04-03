package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

import java.util.Set;

@CommandLine.Command(name = "remove", description = "Remove node from cluster")
public class ClusterRemoveCommand implements Runnable {

@CommandLine.Parameters(index = "0", description = "Node URL")
String node;

@Override
public void run() {
try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (!nodes.contains(node)) {
ConsoleDisplay.getInstance().out("cluster remove: node not found: " + node);
return;
}
nodes.remove(node);
ClusterConfig.writeClusterNodes(nodes);
ConsoleDisplay.getInstance().out("cluster remove: " + node);
} catch (Exception e) {
ConsoleDisplay.getInstance().out("cluster remove: failed to update cluster config: " + e.getMessage());
}
}
}
