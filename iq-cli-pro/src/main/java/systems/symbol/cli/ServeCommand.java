package systems.symbol.cli;

import picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.kernel.KernelException;
import systems.symbol.runtime.ServerRuntimeManager;
import systems.symbol.runtime.ServerRuntimeManagerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Start embedded REST API and MCP servers from CLI
 * 
 * Usage:
 *   iq serve --api# Start REST API server on :8080
 *   iq serve --mcp# Start MCP server on :3000
 *   iq serve --api --mcp  # Start both
 *   iq serve --api --api-port 9000# Custom API port
 *   iq serve --mcp --mcp-port 3001# Custom MCP port
 */
@CommandLine.Command(name = "serve", description = "Start embedded servers (REST API, MCP)")
public class ServeCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(ServeCommand.class);

@CommandLine.Option(names = {"--api"}, description = "Start REST API server", defaultValue = "false")
boolean startApi = false;

@CommandLine.Option(names = {"--mcp"}, description = "Start MCP server", defaultValue = "false")
boolean startMcp = false;

@CommandLine.Option(names = {"--api-port"}, description = "REST API port", defaultValue = "8080")
int apiPort = 8080;

@CommandLine.Option(names = {"--mcp-port"}, description = "MCP server port", defaultValue = "3000")
int mcpPort = 3000;

@CommandLine.Option(names = {"--restful-api-port"}, description = "Legacy alias for --api-port", defaultValue = "-1")
int legacyApiPort = -1;

@CommandLine.Option(names = {"-h", "--host"}, description = "Server bind address", defaultValue = "0.0.0.0")
String host = "0.0.0.0";

private volatile AtomicBoolean running = new AtomicBoolean(false);
private ServerRuntimeManager runtimeManager;

public ServeCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
display("iq.serve.failed: not initialized");
return "error:uninitialized";
}

// Handle legacy option
if (legacyApiPort > 0) {
apiPort = legacyApiPort;
}

// Must start at least one server
if (!startApi && !startMcp) {
display("iq.serve: missing --api or --mcp flag");
display("  Usage: iq serve --api [--api-port PORT]");
display("  Usage: iq serve --mcp [--mcp-port PORT]");
display("  Usage: iq serve --api --mcp");
return "error:no-servers-requested";
}

display("iq.serve: starting embedded servers");
display("  host: " + host);

try {
// Get the runtime manager
this.runtimeManager = ServerRuntimeManagerFactory.getInstance();
this.running.set(true);

// Start REST API server if requested
if (startApi) {
display("  starting REST API server on " + host + ":" + apiPort);
boolean apiStarted = runtimeManager.start("rest-api", apiPort);
if (!apiStarted) {
log.warn("REST API server may have failed to start");
}
}

// Start MCP server if requested
if (startMcp) {
display("  starting MCP server on " + host + ":" + mcpPort);
boolean mcpStarted = runtimeManager.start("mcp", mcpPort);
if (!mcpStarted) {
log.warn("MCP server may have failed to start");
}
}

// Install shutdown hook for graceful shutdown (Ctrl-C)
installShutdownHook();

// Wait for servers to run
display("");
display("==================================================");
display("IQ Embedded Servers Running");
display("==================================================");
if (startApi) {
display("  REST API:  http://" + host + ":" + apiPort);
}
if (startMcp) {
display("  MCP Server: " + host + ":" + mcpPort);
display("  Clients:   See MCP documentation for integration");
}
display("");
display("Press Ctrl-C to shutdown gracefully");
display("==================================================");
display("");

// Block until shutdown signal
synchronized (this) {
while (running.get()) {
try {
wait(1000);  // Check every second
} catch (InterruptedException e) {
log.debug("Interrupted, checking shutdown state");
}
}
}

display("");
display("iq.serve: shutdown initiated");
return "shutdown:graceful";

} catch (KernelException e) {
log.error("iq.serve.kernel_error: {}", e.getMessage(), e);
display("iq.serve.error: Kernel initialization failed");
return "error:kernel";
} catch (Exception e) {
log.error("iq.serve.error: {}", e.getMessage(), e);
display("iq.serve.error: " + e.getMessage());
return "error:runtime";
} finally {
shutdown();
}
}

/**
 * Install shutdown hook for graceful shutdown on Ctrl-C
 */
private void installShutdownHook() {
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
log.info("iq.serve.shutdown_signal: received");
display("\niq.serve.shutdown: starting graceful shutdown...");

synchronized (this) {
running.set(false);
notifyAll();
}

// Stop servers via runtime manager
if (runtimeManager != null) {
try {
if (startApi) {
log.info("iq.serve.api.stop: stopping REST API");
runtimeManager.stop("rest-api", apiPort);
}
if (startMcp) {
log.info("iq.serve.mcp.stop: stopping MCP");
runtimeManager.stop("mcp", mcpPort);
}
} catch (Exception e) {
log.warn("Error during server shutdown: {}", e.getMessage());
}
}
}, "iq-shutdown-hook"));
}

/**
 * Gracefully shutdown all servers
 */
private void shutdown() {
log.info("iq.serve: final shutdown");
if (runtimeManager != null) {
try {
if (startApi) {
runtimeManager.stop("rest-api", apiPort);
}
if (startMcp) {
runtimeManager.stop("mcp", mcpPort);
}
} catch (Exception e) {
log.warn("Error during shutdown: {}", e.getMessage());
}
}
}
}
