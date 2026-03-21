package systems.symbol.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RealmCommand: list realms, query realms.
 */
class RealmCommandTest {

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
    void testRealmListSucceeds() {
        TestCLIExecutor.CLITestResult result = executor.run("realm", "list");
        assertEquals(0, result.exitCode, "realm list should succeed");
        assertNotNull(result.stdout);
    }

    @Test
    void testRealmQueryWithValidSparqlSucceeds() {
        // Query for all things (should return empty or some results)
        TestCLIExecutor.CLITestResult result = executor.run("realm", "query", "ASK { ?x a ?y }");
        assertEquals(0, result.exitCode, "valid SPARQL query should succeed");
    }

    @Test
    void testRealmQueryWithInvalidSparqlFails() {
        // Invalid SPARQL should fail
        TestCLIExecutor.CLITestResult result = executor.run("realm", "query", "invalid sparql )))");
        assertNotEquals(0, result.exitCode, "invalid SPARQL should fail");
    }
}
