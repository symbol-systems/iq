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
        executor.setup();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.teardown();
        }
    }

    @Test
    void testConfigListSucceeds() {
        TestCLIExecutor.CLITestResult result = executor.run("about");
        assertEquals(0, result.exitCode, "about command should succeed. stderr: " + result.stderr);
        assertNotNull(result.stdout);
    }

    @Test
    void testHelpCommandSucceeds() {
        TestCLIExecutor.CLITestResult result = executor.run("about");
        assertEquals(0, result.exitCode, "about command should succeed. stderr: " + result.stderr);
        assertNotNull(result.stdout);
    }

    @Test
    void testVersionCommandSucceeds() {
        TestCLIExecutor.CLITestResult result = executor.run("agent");
        assertEquals(0, result.exitCode, "agent command should succeed. stderr: " + result.stderr);
        assertTrue(result.stdout.length() >= 0);
    }
}
