package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;

@CommandLine.Command(name = "cluster", description = "Manage server cluster nodes", subcommands = {
ClusterListCommand.class,
ClusterAddCommand.class,
ClusterRemoveCommand.class
})
public class ClusterCommand implements Runnable {

@Override
public void run() {
ConsoleDisplay.getInstance().out("Use --help for cluster commands");
}
}
