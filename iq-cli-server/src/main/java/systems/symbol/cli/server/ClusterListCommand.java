package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.io.ConsoleDisplay;

@CommandLine.Command(name = "list", description = "List cluster nodes")
public class ClusterListCommand implements Runnable {

@Override
public void run() {
ConsoleDisplay.getInstance().out("cluster list: no nodes configured");
}
}
