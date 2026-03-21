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
executor.setup();
}

@AfterEach
void tearDown() {
if (executor != null) {
executor.teardown();
}
}

@Test
void testRealmListSucceeds() {
TestCLIExecutor.CLITestResult result = executor.run("agent");
assertEquals(0, result.exitCode, "agent command should succeed. stderr: " + result.stderr);
assertNotNull(result.stdout);
}

@Test
void testRealmQueryWithValidSparqlSucceeds() {
// Query for all things using SPARQL command - must be a tuple query (SELECT)
TestCLIExecutor.CLITestResult result = executor.run("sparql", "SELECT ?x WHERE { ?x a ?y } LIMIT 1");
assertEquals(0, result.exitCode, "valid SPARQL query should succeed. stderr: " + result.stderr);
}

@Test
void testRealmQueryWithInvalidSparqlFails() {
// Invalid SPARQL should fail
TestCLIExecutor.CLITestResult result = executor.run("sparql", "invalid sparql )))");
assertNotEquals(0, result.exitCode, "invalid SPARQL should fail");
}
}
