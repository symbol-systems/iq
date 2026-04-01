package systems.symbol.cli.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.platform.runtime.ServerDump;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "dump", description = "Dump runtime diagnostics")
public class ServerDumpCommand implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerDumpCommand.class);

    @CommandLine.Option(names = {"--path"}, description = "Dump output path", defaultValue = "./tmp/iq-server-dump.tar.gz")
    String path;

    @CommandLine.ParentCommand
    ServerRuntimeScope parent;

    @Override
    public void run() {
        String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
        ServerDump result = ServerRuntimeManagerFactory.getInstance().dump(runtimeType, path);
        log.info("dump {} => {}", runtimeType, result);
    }
}
