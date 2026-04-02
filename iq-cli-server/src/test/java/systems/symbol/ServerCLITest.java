package systems.symbol;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import systems.symbol.cli.server.ServerCommand;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerCLITest {

@Test
public void testServerCommandHelp() {
ServerCommand serverCommand = new ServerCommand(null);
CommandLine cmd = new CommandLine(serverCommand);
String usage = cmd.getUsageMessage();
assertTrue(usage.contains("server"));
assertTrue(usage.contains("api"));
assertTrue(usage.contains("mcp"));
assertTrue(usage.contains("cluster"));
}

@Test
public void testClusterCommandOutput() {
java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
java.io.PrintStream original = System.out;
System.setOut(new java.io.PrintStream(output));
try {
new systems.symbol.cli.server.ClusterCommand().run();
assertTrue(output.toString().contains("Use --help for cluster commands"));
output.reset();
new systems.symbol.cli.server.ClusterListCommand().run();
assertTrue(output.toString().contains("cluster list: no nodes configured"));
} finally {
System.setOut(original);
}
}
}

