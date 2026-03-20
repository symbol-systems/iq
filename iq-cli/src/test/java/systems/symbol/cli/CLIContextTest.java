package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class CLIContextTest {

@Test
public void kernelAndCLIContextIntegration() throws Exception {
File home = Files.createTempDirectory("iq-cli-test").toFile();
home.deleteOnExit();

I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();
CLIContext context = new CLIContext(kernel);

assertNotNull(context.getKernelContext());
assertEquals(home.getAbsolutePath(), context.getKernelContext().getHome().getAbsolutePath());

assertTrue(context.getKernelContext().isInitialized());
assertNotNull(context.getAssetsHome());
assertNotNull(context.getBackupsHome());
assertNotNull(context.getPublicHome());

assertTrue(context.home.exists());

// close should shut repository (if any) and kernel
context.close();
assertDoesNotThrow(kernel::stop);
}
}
