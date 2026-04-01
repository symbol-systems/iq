package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "cluster", description = "Manage server cluster nodes", subcommands = {
        ClusterListCommand.class,
        ClusterAddCommand.class,
        ClusterRemoveCommand.class
})
public class ClusterCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClusterCommand.class);

    @Override
    public void run() {
        log.info("Use --help for cluster commands");
    }
}
