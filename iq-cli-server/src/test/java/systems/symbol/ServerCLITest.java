package systems.symbol;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import systems.symbol.cli.server.ServerCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerCLITest {

@Test
public void testServerCommandHelp() {
ServerCommand serverCommand = new ServerCommand(null);
CommandLine cmd = new CommandLine(serverCommand);
String usage = cmd.getUsageMessage();
assertEquals(true, usage.contains("server"));
}
}
