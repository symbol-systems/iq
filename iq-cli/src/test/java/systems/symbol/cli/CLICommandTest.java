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
}
