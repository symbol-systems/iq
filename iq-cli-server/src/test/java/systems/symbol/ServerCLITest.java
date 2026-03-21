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
}
