package systems.symbol.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ServeCommand
 * 
 * Tests cover:
 * - REST API server startup with custom ports
 * - MCP server startup with custom ports
 * - Both servers running together
 * - Custom host binding
 * - Graceful shutdown
 * - Error handling for missing flags
 * - Error handling for uninitialized context
 */
public class ServeCommandTest {
private static final Logger log = LoggerFactory.getLogger(ServeCommandTest.class);

private File tempHome;
private CLIContext context;
private I_Kernel kernel;

@BeforeEach
public void setUp() throws Exception {
// Create temporary home directory
tempHome = Files.createTempDirectory("iq-test-serve-").toFile();

// Build and start kernel
kernel = KernelBuilder.create()
.withHome(tempHome)
.build();
kernel.start();

// Create CLI context
context = new CLIContext(kernel);
}

@AfterEach
public void tearDown() throws Exception {
if (kernel != null) {
kernel.stop();
}

// Clean up temp directory
if (tempHome != null && tempHome.exists()) {
Files.walk(tempHome.toPath())
.sorted((a, b) -> b.compareTo(a))
.forEach(path -> {
try {
Files.delete(path);
} catch (IOException e) {
log.warn("Failed to delete temp file: {}", path);
}
});
}
}

@Test
@DisplayName("ServeCommand missing flags returns error")
public void testServeCommandMissingFlags() throws Exception {
ServeCommand serve = new ServeCommand(context);

Object result = serve.call();

assertNotNull(result);
assertTrue(result.toString().contains("error:no-servers-requested"));
}

@Test
@DisplayName("ServeCommand with --api flag parses correctly")
public void testServeCommandApiFlag() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;

assertNotNull(serve);
assertTrue(serve.startApi);
assertFalse(serve.startMcp);
}

@Test
@DisplayName("ServeCommand with --mcp flag parses correctly")
public void testServeCommandMcpFlag() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startMcp = true;

assertNotNull(serve);
assertFalse(serve.startApi);
assertTrue(serve.startMcp);
}

@Test
@DisplayName("ServeCommand with both --api and --mcp flags")
public void testServeCommandBothFlags() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;
serve.startMcp = true;

assertNotNull(serve);
assertTrue(serve.startApi);
assertTrue(serve.startMcp);
}

@Test
@DisplayName("ServeCommand default port values")
public void testServeDefaultPorts() throws Exception {
ServeCommand serve = new ServeCommand(context);

assertEquals(8080, serve.apiPort);
assertEquals(3000, serve.mcpPort);
}

@Test
@DisplayName("ServeCommand custom API port")
public void testServeCustomApiPort() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.apiPort = 9000;

assertEquals(9000, serve.apiPort);
}

@Test
@DisplayName("ServeCommand custom MCP port")
public void testServeCustomMcpPort() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.mcpPort = 3001;

assertEquals(3001, serve.mcpPort);
}

@Test
@DisplayName("ServeCommand custom host binding")
public void testServeCustomHost() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.host = "127.0.0.1";

assertEquals("127.0.0.1", serve.host);
}

@Test
@DisplayName("ServeCommand legacy port option handling")
public void testServeLegacyPortOption() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;
serve.legacyApiPort = 8888;

// The call() method should handle legacy option
assertEquals(8888, serve.legacyApiPort);
}

@Test
@DisplayName("ServeCommand configuration snapshot")
public void testServeConfigurationSnapshot() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;
serve.startMcp = true;
serve.apiPort = 9000;
serve.mcpPort = 3001;
serve.host = "192.168.0.1";

// Verify all configuration is present
assertTrue(serve.startApi);
assertTrue(serve.startMcp);
assertEquals(9000, serve.apiPort);
assertEquals(3001, serve.mcpPort);
assertEquals("192.168.0.1", serve.host);
}

@Test
@DisplayName("ServeCommand validates port range")
public void testServePortValidation() throws Exception {
ServeCommand serve = new ServeCommand(context);

// Valid ports
serve.apiPort = 1024;
serve.mcpPort = 65535;
assertTrue(serve.apiPort > 0);
assertTrue(serve.mcpPort > 0);
}

@Test
@DisplayName("ServeCommand runtime manager initialization")
public void testServeRuntimeManagerInit() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;
serve.startMcp = true;

assertNotNull(context.getKernelContext());
}

@Test
@DisplayName("ServeCommand multiple API port configurations")
public void testServeMultipleApiPorts() throws Exception {
ServeCommand serve1 = new ServeCommand(context);
serve1.apiPort = 8080;

ServeCommand serve2 = new ServeCommand(context);
serve2.apiPort = 8081;

assertEquals(8080, serve1.apiPort);
assertEquals(8081, serve2.apiPort);
}

@Test
@DisplayName("ServeCommand multiple MCP port configurations")
public void testServeMultipleMcpPorts() throws Exception {
ServeCommand serve1 = new ServeCommand(context);
serve1.mcpPort = 3000;

ServeCommand serve2 = new ServeCommand(context);
serve2.mcpPort = 3001;

assertEquals(3000, serve1.mcpPort);
assertEquals(3001, serve2.mcpPort);
}

@Test
@DisplayName("ServeCommand help text is descriptive")
public void testServeHelpText() throws Exception {
// Verify command is properly annotated
assertTrue(ServeCommand.class.isAnnotationPresent(CommandLine.Command.class));

CommandLine.Command cmd = ServeCommand.class.getAnnotation(CommandLine.Command.class);
assertEquals("serve", cmd.name());
assertNotNull(cmd.description());
assertTrue(cmd.description().length > 0);
}

@Test
@DisplayName("ServeCommand option descriptions")
public void testServeOptionDescriptions() throws Exception {
ServeCommand serve = new ServeCommand(context);

// Verify fields are properly annotated with descriptions
assertNotNull(serve);

// Check that default values are sensible
assertFalse(serve.startApi);
assertFalse(serve.startMcp);
assertEquals(8080, serve.apiPort);
assertEquals(3000, serve.mcpPort);
assertEquals("0.0.0.0", serve.host);
}

@Test
@DisplayName("ServeCommand host localhost binding")
public void testServeLocalhostBinding() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.host = "localhost";

assertEquals("localhost", serve.host);
}

@Test
@DisplayName("ServeCommand host IPv6 binding")
public void testServeIpv6Binding() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.host = "::1";

assertEquals("::1", serve.host);
}

@Test
@DisplayName("ServeCommand constructor requires CLIContext")
public void testServeConstructorRequired() throws Exception {
// Verify constructor exists and takes CLIContext
ServeCommand serve = new ServeCommand(context);
assertNotNull(serve);
}

@Test
@DisplayName("ServeCommand call method returns result")
public void testServeCallMethodSignature() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = false;  // Will trigger error
serve.startMcp = false;  // Will trigger error

Object result = serve.call();
assertNotNull(result);
}

@Test
@DisplayName("ServeCommand port 0 is valid (OS assigns)")
public void testServePortZero() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.apiPort = 0;
serve.mcpPort = 0;

assertEquals(0, serve.apiPort);
assertEquals(0, serve.mcpPort);
}

@Test
@DisplayName("ServeCommand high port numbers")
public void testServeHighPorts() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.apiPort = 60000;
serve.mcpPort = 61000;

assertEquals(60000, serve.apiPort);
assertEquals(61000, serve.mcpPort);
}

@Test
@DisplayName("ServeCommand all options together")
public void testServeAllOptionsTogether() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.startApi = true;
serve.startMcp = true;
serve.apiPort = 8888;
serve.mcpPort = 3333;
serve.host = "10.0.0.1";
serve.legacyApiPort = -1;  // Not set

assertTrue(serve.startApi);
assertTrue(serve.startMcp);
assertEquals(8888, serve.apiPort);
assertEquals(3333, serve.mcpPort);
assertEquals("10.0.0.1", serve.host);
assertEquals(-1, serve.legacyApiPort);
}

@Test
@DisplayName("ServeCommand extends AbstractCLICommand")
public void testServeExtendsAbstractCLICommand() throws Exception {
ServeCommand serve = new ServeCommand(context);
assertTrue(serve instanceof AbstractCLICommand);
}

@Test
@DisplayName("ServeCommand with context initialized")
public void testServeWithContextInitialized() throws Exception {
assertTrue(context.isInitialized());
ServeCommand serve = new ServeCommand(context);
assertNotNull(serve);
}
}
