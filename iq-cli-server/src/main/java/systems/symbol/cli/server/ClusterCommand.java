package systems.symbol.cli.server;

import picocli.CommandLine;

@CommandLine.Command(name = "cluster", description = "Manage server cluster nodes", subcommands = {
ClusterListCommand.class,
ClusterAddCommand.class,
ClusterRemoveCommand.class
})
public class ClusterCommand implements Runnable {

@Override
public void run() {
System.out.println("Use --help for cluster commands");
}
}
