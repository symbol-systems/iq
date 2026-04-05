package systems.symbol.cli.server;

import picocli.CommandLine;

import java.util.Set;

@CommandLine.Command(name = "add", description = "Add node to cluster")
public class ClusterAddCommand implements Runnable {

@CommandLine.ParentCommand
private ClusterCommand parent;

@CommandLine.Parameters(index = "0", description = "Node URL")
String node;

@Override
public void run() {
if (!ClusterConfig.isValidNode(node)) {
display("cluster add: invalid node URL: " + node);
return;
}

try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (nodes.contains(node)) {
display("cluster add: node already configured: " + node);
return;
}
nodes.add(node);
ClusterConfig.writeClusterNodes(nodes);
display("cluster add: " + node);
} catch (Exception e) {
display("cluster add: failed to update cluster config: " + e.getMessage());
}
}

private void display(String message) {
if (parent != null && parent.getContext() != null) {
parent.getContext().display(message);
}
}
}
