package systems.symbol.cli.server;

import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List cluster nodes")
public class ClusterListCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("cluster list: no nodes configured");
    }
}
