package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

@CommandLine.Command(name = "remove", description = "Remove node from cluster")
public class ClusterRemoveCommand implements Runnable {

@CommandLine.Parameters(index = "0", description = "Node ID/path")
String node;

@Override
public void run() {
ConsoleDisplay.getInstance().out("cluster remove: " + node);
}
}
