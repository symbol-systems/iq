package systems.symbol.cli.server;

import picocli.CommandLine;
import systems.symbol.io.ConsoleDisplay;
import systems.symbol.runtime.ServerDump;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

@CommandLine.Command(name = "dump", description = "Dump runtime diagnostics")
public class ServerDumpCommand implements Runnable {

@CommandLine.Option(names = {"--path"}, description = "Dump output path", defaultValue = "/tmp/iq-server-dump.tar.gz")
String path;

@CommandLine.Option(names = "--port", description = "Port to target", defaultValue = "0")
int port;

@CommandLine.ParentCommand
ServerRuntimeScope parent;

@Override
public void run() {
String runtimeType = parent != null ? parent.getRuntimeType() : "unknown";
ServerDump result;
if (port > 0) {
result = ServerRuntimeManagerFactory.getInstance().dump(runtimeType, path);
} else {
result = ServerRuntimeManagerFactory.getInstance().dump(runtimeType, path);
}
ConsoleDisplay.getInstance().outf("dump %s%s => %s%n", runtimeType, port > 0 ? ":" + port : "", result);
}
}
