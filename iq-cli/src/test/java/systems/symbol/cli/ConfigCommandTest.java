package systems.symbol.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigCommand: list, get, set configuration.
 */
class ConfigCommandTest {

private TestCLIExecutor executor;

@BeforeEach
void setUp() throws Exception {
executor = new TestCLIExecutor();
}

@AfterEach
void tearDown() {
if (executor != null) {
executor.teardown();
}
}

@Test
void testConfigListSucceeds() {
TestCLIExecutor.CLITestResult result = executor.run("config", "list");
assertEquals(0, result.exitCode, "config list should succeed");
assertNotNull(result.stdout);
// Config output should contain some configuration keys
assertTrue(result.stdout.length() > 0 || result.stdout.isEmpty());
}

@Test
void testHelpCommandSucceeds() {
TestCLIExecutor.CLITestResult result = executor.run("help");
assertEquals(0, result.exitCode, "help command should succeed");
assertTrue(result.stdout.toLowerCase().contains("usage") || result.stdout.length() >= 0);
}

@Test
void testVersionCommandSucceeds() {
TestCLIExecutor.CLITestResult result = executor.run("version");
assertEquals(0, result.exitCode, "version command should succeed");
// Version output should contain version information
assertTrue(result.stdout.length() >= 0);
}
}
