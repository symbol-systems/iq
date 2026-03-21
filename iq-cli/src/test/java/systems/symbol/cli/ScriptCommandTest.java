package systems.symbol.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScriptCommand: list, execute scripts.
 */
class ScriptCommandTest {

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
    void testScriptListSucceeds() {
        // Test script --list instead of script list
        TestCLIExecutor.CLITestResult result = executor.run("script", "--list");
        assertEquals(0, result.exitCode, "script --list should succeed. stderr: " + result.stderr);
        assertNotNull(result.stdout);
    }

    @Test
    void testScriptCommandWithMissingFileFailsGracefully() {
        // Try to run a script that doesn't exist
        TestCLIExecutor.CLITestResult result = executor.run("script", "execute", "/nonexistent/script.sparql");
        assertNotEquals(0, result.exitCode, "executing non-existent script should fail");
    }

    @Test
    void testGroovyNotSupportedInCLI() {
        // Groovy is not supported in iq-cli (only in iq-cli-pro)
        TestCLIExecutor.CLITestResult result = executor.run("script", "execute", "/nonexistent/script.groovy");
        assertNotEquals(0, result.exitCode, "groovy execution should not be supported in iq-cli");
    }
}
