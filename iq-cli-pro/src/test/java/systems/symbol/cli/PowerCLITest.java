package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import systems.symbol.CLI;
import systems.symbol.PowerCLI;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerCLITest {

@Test
public void testPowerCLIRegistration() throws Exception {
File home = Files.createTempDirectory("iq-cli-pro-test").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);
PowerCLI powerCLI = new PowerCLI();
CommandLine cmd = powerCLI.getCommandLine(context);

assertNotNull(cmd);
assertTrue(cmd.getSubcommands().containsKey("run"));
assertTrue(cmd.getSubcommands().containsKey("trigger"));
assertTrue(cmd.getSubcommands().containsKey("models"));
assertTrue(cmd.getSubcommands().containsKey("trust"));
assertTrue(cmd.getSubcommands().containsKey("boot"));
} finally {
kernel.stop();
}
}

@Test
public void testProCommandsExecuteNoExceptions() throws Exception {
File home = Files.createTempDirectory("iq-cli-pro-test2").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);
assertDoesNotThrow(() -> new BootCommand(context).call(), "Boot should not throw for initialized context");
assertNull(new RunCommand(context, new CommandLine(new CLI())).call());
assertEquals("triggered", new TriggerCommand(context).call());
assertNotNull(new TrustCommand(context).call());
} finally {
kernel.stop();
}
}
}
