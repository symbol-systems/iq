package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerDump;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "dump", description = "Dump runtime diagnostics")
public class ServerDumpCommand implements Runnable {

    @CommandLine.Option(names = {"--path"}, description = "Dump output path", defaultValue = "/tmp/iq-server-dump.tar.gz")
    String path;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        ServerDump result = ServerRuntimeManagerFactory.getInstance().dump(runtimeType, path);
        System.out.println("dump " + runtimeType + " => " + result);
    }
}
