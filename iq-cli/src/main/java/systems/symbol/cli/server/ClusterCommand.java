package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.cli.CLIContext;

@CommandLine.Command(name = "cluster", description = "Manage server cluster nodes", subcommands = {
ClusterListCommand.class,
ClusterAddCommand.class,
ClusterRemoveCommand.class
})
public class ClusterCommand implements Runnable {

@CommandLine.ParentCommand
private ServerCommand parent;

@Override
public void run() {
if (getContext() != null) {
getContext().display("Use --help for cluster commands");
}
}

public CLIContext getContext() {
if (parent != null) {
return parent.getContext();
}
return null;
}
}
