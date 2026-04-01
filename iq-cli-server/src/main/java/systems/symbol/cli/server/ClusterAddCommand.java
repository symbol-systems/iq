package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "add", description = "Add node to cluster")
public class ClusterAddCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClusterAddCommand.class);

    @CommandLine.Parameters(index = "0", description = "Node ID/path")
    String node;

    @Override
    public void run() {
        try {
            ClusterNodeManager manager = ClusterNodeManager.getInstance();
            if (manager.nodeExists(node)) {
                log.warn("cluster.add.duplicate: {} already exists", node);
                return;
            }
            
            manager.addNode(node);
            java.util.List<String> allNodes = manager.listNodes();
            log.info("cluster.add: {} -> nodes={}", node, allNodes);
            System.out.println("Added node: " + node);
            System.out.println("Cluster nodes: " + allNodes);
        } catch (Exception e) {
            log.error("cluster.add.error: {}", e.getMessage(), e);
        }
    }
}
