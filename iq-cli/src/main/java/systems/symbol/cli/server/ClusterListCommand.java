package systems.symbol.cli.server;

import picocli.CommandLine;

import java.util.Set;

@CommandLine.Command(name = "list", description = "List cluster nodes")
public class ClusterListCommand implements Runnable {

@CommandLine.ParentCommand
private ClusterCommand parent;

@Override
public void run() {
try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (nodes.isEmpty()) {
display("cluster list: no nodes configured");
return;
}
display("cluster list: " + nodes.size() + " node(s)");
for (String node : nodes) {
display(" - " + node);
}
} catch (Exception e) {
display("cluster list: failed to read cluster config: " + e.getMessage());
}
}

private void display(String message) {
if (parent != null && parent.getContext() != null) {
parent.getContext().display(message);
}
}
}
