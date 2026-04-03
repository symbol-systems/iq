package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

import java.util.Set;

@CommandLine.Command(name = "list", description = "List cluster nodes")
public class ClusterListCommand implements Runnable {

@Override
public void run() {
try {
Set<String> nodes = ClusterConfig.readClusterNodes();
if (nodes.isEmpty()) {
ConsoleDisplay.getInstance().out("cluster list: no nodes configured");
return;
}
ConsoleDisplay.getInstance().out("cluster list: " + nodes.size() + " node(s)");
for (String node : nodes) {
ConsoleDisplay.getInstance().out(" - " + node);
}
} catch (Exception e) {
ConsoleDisplay.getInstance().out("cluster list: failed to read cluster config: " + e.getMessage());
}
}
}
