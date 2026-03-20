package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
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

int exitCode = cmd.execute("help");
String usage = sw.toString();

assertEquals(0, exitCode);
assertTrue(usage.toLowerCase().contains("usage"));
assertTrue(usage.toLowerCase().contains("options"));
assertTrue(usage.toLowerCase().contains("server"));

context.close();

} finally {
kernel.stop();
}
}
}
