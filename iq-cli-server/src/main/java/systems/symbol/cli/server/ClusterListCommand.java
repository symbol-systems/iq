package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.util.List;

@CommandLine.Command(name = "list", description = "List cluster nodes")
public class ClusterListCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClusterListCommand.class);

    @Override
    public void run() {
        try {
            ClusterNodeManager manager = ClusterNodeManager.getInstance();
            List<String> nodes = manager.listNodes();
            
            if (nodes.isEmpty()) {
                log.info("cluster.list: no nodes configured");
                System.out.println("No cluster nodes configured");
                return;
            }
            
            log.info("cluster.list: {} nodes", nodes.size());
            System.out.println("\nCluster Nodes (" + nodes.size() + "):");
            System.out.println("─────────────────────────────────");
            for (int i = 0; i < nodes.size(); i++) {
                System.out.printf("%2d. %s%n", i + 1, nodes.get(i));
            }
            System.out.println("─────────────────────────────────");
        } catch (Exception e) {
            log.error("cluster.list.error: {}", e.getMessage(), e);
        }
    }
}
