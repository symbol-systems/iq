package systems.symbol.cli.server;

import picocli.CommandLine;

import java.util.Set;

@CommandLine.Command(name = "remove", description = "Remove node from cluster")
public class ClusterRemoveCommand implements Runnable {

@CommandLine.ParentCommand
private ClusterCommand parent;

@CommandLine.Parameters(index = "0", description = "Node URL")
String node;

@Override
public void run() {
try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (!nodes.contains(node)) {
display("cluster remove: node not found: " + node);
return;
}
nodes.remove(node);
ClusterConfig.writeClusterNodes(nodes);
display("cluster remove: " + node);
} catch (Exception e) {
display("cluster remove: failed to update cluster config: " + e.getMessage());
}
}

private void display(String message) {
if (parent != null && parent.getContext() != null) {
parent.getContext().display(message);
}
}
}
