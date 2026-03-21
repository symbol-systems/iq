package systems.symbol.cli;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import systems.symbol.CLI;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test harness for CLI command execution.
 * 
 * Provides in-process CLI dispatch without subprocess overhead.
 * Captures stdout/stderr and exit code for assertion.
 */
public class TestCLIExecutor {

public static class CLITestResult {
public final int exitCode;
public final String stdout;
public final String stderr;
public final long executionTimeMs;

public CLITestResult(int exitCode, String stdout, String stderr, long executionTimeMs) {
this.exitCode = exitCode;
this.stdout = stdout;
this.stderr = stderr;
this.executionTimeMs = executionTimeMs;
}

@Override
public String toString() {
return String.format("CLITestResult{exitCode=%d, stdout=%s, stderr=%s, time=%dms}",
exitCode, stdout, stderr, executionTimeMs);
}
}

private File home;
private I_Kernel kernel;
private CLIContext context;

@BeforeEach
public void setup() throws Exception {
home = Files.createTempDirectory("iq-cli-executor").toFile();
home.deleteOnExit();

kernel = KernelBuilder.create().withHome(home).build();
kernel.start();
context = new CLIContext(kernel);
assertNotNull(context);
}

@AfterEach
public void teardown() {
if (context != null) {
context.close();
}
if (kernel != null) {
kernel.stop();
}
}

/**
 * Run a CLI command with arguments.
 * 
 * @param args Command line arguments (e.g., "help", "version", "config", "list")
 * @return CLITestResult with exit code, stdout, stderr, execution time
 */
public CLITestResult run(String... args) {
long startTime = System.currentTimeMillis();
int exitCode = 0;
String stdout = "";
String stderr = "";

try {
// Capture output
StringWriter outWriter = new StringWriter();
StringWriter errWriter = new StringWriter();
PrintWriter outPrintWriter = new PrintWriter(outWriter);
PrintWriter errPrintWriter = new PrintWriter(errWriter);

CLI cli = new CLI();
CommandLine cmd = cli.getCommandLine(context);

cmd.setOut(outPrintWriter);
cmd.setErr(errPrintWriter);

// Execute command
exitCode = cmd.execute(args);

stdout = outWriter.toString();
stderr = errWriter.toString();

} catch (Exception e) {
exitCode = 1;
stderr = e.getClass().getSimpleName() + ": " + e.getMessage();
}

long executionTime = System.currentTimeMillis() - startTime;
return new CLITestResult(exitCode, stdout, stderr, executionTime);
}

/**
 * Get the underlying CLIContext for direct access if needed.
 */
public CLIContext getContext() {
return context;
}

/**
 * Get the home directory for the test.
 */
public File getHomeDir() {
return home;
}

/**
 * Get the kernel for the test.
 */
public I_Kernel getKernel() {
return kernel;
}

@Test
public void inferCommandExecutesSparqlInsert() throws Exception {
File script = new File(home, "infer/index.sparql");
script.getParentFile().mkdirs();
Files.writeString(script.toPath(), "INSERT DATA { <urn:test> <urn:p> \"x\" }", StandardCharsets.UTF_8);

InferCommand inferCommand = new InferCommand(context);
Object result = inferCommand.call();
assertEquals(0, result);

try (RepositoryConnection conn = context.getRepository().getConnection()) {
boolean exists = conn.prepareBooleanQuery("ASK { <urn:test> <urn:p> \"x\" }").evaluate();
assertTrue(exists, "Triple inserted by infer command should exist");
}
}

@Test
public void renderCommandDoesNotFailWhenNoModels() throws Exception {
RenderCommand renderCommand = new RenderCommand(context);
Object result = renderCommand.call();
assertEquals(0, result);
}

@Test
public void agentCommandListAndTriggerStubWorks() throws Exception {
AgentCommand agentCommand = new AgentCommand(context);
Object result = agentCommand.call();
assertEquals(0, result);

AgentCommand triggerCommand = new AgentCommand(context);
triggerCommand.actor = "dummy";
triggerCommand.intent = "dummy";
triggerCommand.trigger = true;
result = triggerCommand.call();
assertEquals(0, result);
}
}

