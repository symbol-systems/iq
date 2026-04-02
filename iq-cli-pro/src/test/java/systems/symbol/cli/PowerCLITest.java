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
public void testRunBootTriggerTrustAndModelsBehavior() throws Exception {
File home = Files.createTempDirectory("iq-cli-pro-test3").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);

// boot should return empty with no actors
assertEquals("boot:empty", new BootCommand(context).call());

// script run sparql on empty data set (return non-null path output)
File query = File.createTempFile("iq-cli-pro-test", ".sparql");
query.deleteOnExit();
java.nio.file.Files.writeString(query.toPath(), "SELECT * WHERE { ?s ?p ?o } LIMIT 1");

RunCommand run = new RunCommand(context, new CommandLine(new PowerCLI()));
run.path = query.getAbsolutePath();
Object runRes = run.call();
assertEquals("run:" + query.getAbsolutePath() + ":sparql", runRes);

// trigger should at least return triggered
assertEquals("triggered", new TriggerCommand(context).call());

// trust self and list should work
TrustCommand trust = new TrustCommand(context);
assertEquals("trusted:me", trust.call());
trust.identity = "list";
assertEquals("trusted:list", trust.call());

// models command at least returns null when none present
ModelsCommand models = new ModelsCommand(context, new CommandLine(new PowerCLI()));
assertDoesNotThrow(models::call);

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
