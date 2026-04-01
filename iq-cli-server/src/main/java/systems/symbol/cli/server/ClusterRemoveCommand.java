package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", description = "Remove node from cluster")
public class ClusterRemoveCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClusterRemoveCommand.class);

    @CommandLine.Parameters(index = "0", description = "Node ID/path")
    String node;

    @Override
    public void run() {
        try {
            ClusterNodeManager manager = ClusterNodeManager.getInstance();
            if (!manager.nodeExists(node)) {
                log.warn("cluster.remove.notfound: {} does not exist", node);
                return;
            }
            
            manager.removeNode(node);
            java.util.List<String> allNodes = manager.listNodes();
            log.info("cluster.remove: {} removed -> remaining nodes={}", node, allNodes);
            System.out.println("Removed node: " + node);
            if (!allNodes.isEmpty()) {
                System.out.println("Remaining cluster nodes: " + allNodes);
            } else {
                System.out.println("Cluster is now empty");
            }
        } catch (Exception e) {
            log.error("cluster.remove.error: {}", e.getMessage(), e);
        }
    }
}
