package systems.symbol.cli.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterCommandTest extends systems.symbol.cli.TestCLIExecutor {

@Test
void testClusterCommandsAddListRemove() throws Exception {
var result1 = run("server", "cluster", "list");
assertEquals(0, result1.exitCode);
assertTrue(result1.stdout.contains("cluster list: no nodes configured"));

var result2 = run("server", "cluster", "add", "http://localhost:9000");
assertEquals(0, result2.exitCode);
assertTrue(result2.stdout.contains("cluster add: http://localhost:9000"));

var result3 = run("server", "cluster", "list");
assertEquals(0, result3.exitCode);
assertTrue(result3.stdout.contains("cluster list: 1 node(s)"));
assertTrue(result3.stdout.contains("http://localhost:9000"));

var result4 = run("server", "cluster", "remove", "http://localhost:9000");
assertEquals(0, result4.exitCode);
assertTrue(result4.stdout.contains("cluster remove: http://localhost:9000"));

var result5 = run("server", "cluster", "list");
assertEquals(0, result5.exitCode);
assertTrue(result5.stdout.contains("cluster list: no nodes configured"));
}

@Test
void testServerHelpIncludesCluster() throws Exception {
var result = run("server", "--help");
assertEquals(0, result.exitCode);
assertTrue(result.stdout.contains("cluster"), "server --help should list cluster subcommand");
}
}
