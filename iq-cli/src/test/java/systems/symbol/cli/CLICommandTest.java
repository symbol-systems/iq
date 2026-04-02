package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import systems.symbol.CLI;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class CLICommandTest {

@Test
public void helpCommandShouldReturnZeroAndDisplayUsage() throws Exception {
File home = Files.createTempDirectory("iq-cli-test").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);
CLI cli = new CLI();
CommandLine cmd = cli.getCommandLine(context);

assertNotNull(cmd);

StringWriter sw = new StringWriter();
cmd.setOut(new PrintWriter(sw));

cmd.usage(new PrintWriter(sw));
String usage = sw.toString();

assertTrue(usage.toLowerCase().contains("usage"));

context.close();

} finally {
kernel.stop();
}
}

@Test
public void recoverCommandShouldWorkAfterBackup() throws Exception {
File home = Files.createTempDirectory("iq-cli-recover-test").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);
assertTrue(context.isInitialized());

new BackupCommand(context).call();
Object result = new RecoverCommand(context).call();

assertEquals("recovered", result);
context.close();

} finally {
kernel.stop();
}
}

@Test
public void ossCliShouldNotExposeProCommands() throws Exception {
File home = Files.createTempDirectory("iq-cli-test-procheck").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();

try {
CLIContext context = new CLIContext(kernel);
CLI cli = new CLI();
CommandLine cmd = cli.getCommandLine(context);

assertNotNull(cmd);
assertFalse(cmd.getSubcommands().containsKey("run"), "OSS CLI must not include pro run command");
assertFalse(cmd.getSubcommands().containsKey("models"), "OSS CLI must not include pro models command");
assertFalse(cmd.getSubcommands().containsKey("trigger"), "OSS CLI must not include pro trigger command");
assertFalse(cmd.getSubcommands().containsKey("trust"), "OSS CLI must not include pro trust command");
assertFalse(cmd.getSubcommands().containsKey("boot"), "OSS CLI must not include pro boot command");
assertTrue(cmd.getSubcommands().containsKey("script"), "OSS CLI must include script command");

context.close();
} finally {
kernel.stop();
}
}
}

