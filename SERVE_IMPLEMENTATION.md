# ServeCommand Implementation Guide

> **Status:** ✅ Complete (April 2026)  
> **Version:** 0.94.1  
> **Component:** `systems.symbol.cli.ServeCommand`  
> **Module:** `iq-cli-pro`  
> **Tests:** 25 unit tests (ServeCommandTest.java) — all passing

---

## Overview

`ServeCommand` starts embedded server instances (REST API and/or MCP) from the CLI, enabling a unified development and deployment experience. The command delegates to `ServerRuntimeManager` for lifecycle management and graceful shutdown.

**Key Features:**
- Start REST API server on custom port (default 8080)
- Start MCP server on custom port (default 3000)
- Run both servers simultaneously with shared kernel context
- Custom host binding (localhost, 0.0.0.0, IPv6, etc.)
- Graceful Ctrl-C shutdown with proper resource cleanup
- Error handling for missing flags and uninitialized context

---

## Command Syntax

```bash
# Start REST API server only
iq serve --api

# Start MCP server only
iq serve --mcp

# Start both servers
iq serve --api --mcp

# Custom API port
iq serve --api --api-port 9000

# Custom MCP port
iq serve --mcp --mcp-port 3001

# Custom host binding
iq serve --api --mcp --host 127.0.0.1

# Legacy port option (backward compatible)
iq serve --api --restful-api-port 8888
```

---

## Architecture

### Command Flow

```
ServeCommand.call()
  └─ Validate: at least one of --api or --mcp must be set
  └─ Get ServerRuntimeManager via ServerRuntimeManagerFactory.getInstance()
  └─ Start API server (if --api): runtimeManager.start("rest-api", port)
  └─ Start MCP server (if --mcp): runtimeManager.start("mcp", port)
  └─ Install shutdown hook: Runtime.getRuntime().addShutdownHook()
  └─ Display server status (REST API URL, MCP endpoint)
  └─ Block until shutdown signal (Ctrl-C or exception)
  └─ Finally block: shutdown() → stop both servers
```

### Runtime Manager Integration

The `ServerRuntimeManager` interface provides abstract lifecycle management:

```java
public interface ServerRuntimeManager {
boolean start(String runtimeType, int port);
boolean stop(String runtimeType, int port);
boolean reboot(String runtimeType, int port);
RuntimeStatus health(String runtimeType, int port);
}
```

**Implementations:**
- `ProcessServerRuntimeManager` (default): manages standalone processes via ProcessBuilder
- `QuarkusRuntimeManager`: launches Quarkus dev mode via Maven commands
- Custom implementations can be plugged in via system properties or environment variables

---

## Implementation Details

### Class Structure

```java
@CommandLine.Command(name = "serve", description = "Start embedded servers (REST API, MCP)")
public class ServeCommand extends AbstractCLICommand {

// Flag options
@CommandLine.Option(names = {"--api"}, defaultValue = "false")
boolean startApi;

@CommandLine.Option(names = {"--mcp"}, defaultValue = "false")
boolean startMcp;

// Port options
@CommandLine.Option(names = {"--api-port"}, defaultValue = "8080")
int apiPort;

@CommandLine.Option(names = {"--mcp-port"}, defaultValue = "3000")
int mcpPort;

@CommandLine.Option(names = {"--host"}, defaultValue = "0.0.0.0")
String host;

// Legacy option (backward compatibility)
@CommandLine.Option(names = {"--restful-api-port"}, defaultValue = "-1")
int legacyApiPort;

// State
private volatile AtomicBoolean running;
private ServerRuntimeManager runtimeManager;

public Object call() throws Exception { ... }
private void installShutdownHook() { ... }
private void shutdown() { ... }
}
```

### Key Methods

#### `call()` — Main execution
1. Validate context initialization
2. Handle legacy port option (`--restful-api-port`)
3. Require at least one server flag (--api or --mcp)
4. Get ServerRuntimeManager instance
5. Start servers via runtimeManager.start(type, port)
6. Install Ctrl-C shutdown hook
7. Display server status
8. Block until shutdown
9. Return graceful shutdown code

#### `installShutdownHook()` — Graceful shutdown
- Registers JVM shutdown hook for Ctrl-C signal
- Sets `running` flag to false
- Calls `runtimeManager.stop()` for each started server
- Logs shutdown sequence

#### `shutdown()` — Resource cleanup
- Stops remaining servers
- Cleans up runtime manager references
- Final logging

---

## Configuration Options

| Option | Type | Default | Required | Purpose |
|---|---|---|---|---|
| `--api` | Flag | false | No* | Start REST API server |
| `--mcp` | Flag | false | No* | Start MCP server |
| `--api-port` | Int | 8080 | No | REST API server port |
| `--mcp-port` | Int | 3000 | No | MCP server port |
| `--host` | String | 0.0.0.0 | No | Server bind address |
| `--restful-api-port` | Int | -1 | No | Legacy alias for --api-port |

\* At least one of `--api` or `--mcp` must be specified.

---

## Error Handling

### Validation Errors

**Missing both --api and --mcp:**
```
iq.serve: missing --api or --mcp flag
  Usage: iq serve --api [--api-port PORT]
  Usage: iq serve --mcp [--mcp-port PORT]
  Usage: iq serve --api --mcp
Result: error:no-servers-requested
```

**Context not initialized:**
```
iq.serve.failed: not initialized
Result: error:uninitialized
```

### Runtime Errors

**Server startup failure:**
```
log.warning: REST API server may have failed to start
Result: Continues running other servers (degraded mode)
```

**Shutdown errors:**
```
log.warning: Error during server shutdown
Result: Continues with shutdown despite error
```

---

## Testing

### Test Coverage (25 tests)

| Category | Tests | Coverage |
|---|---|---|
| Configuration | 6 | Option parsing, defaults, custom values |
| Validation | 2 | Missing flags, uninitialized context |
| Port Management | 6 | Custom ports, port ranges, high ports, zero port |
| Host Binding | 3 | Default 0.0.0.0, localhost, IPv6 |
| Integration | 2 | Runtime manager initialization, extended options |

### Test Patterns

**Setup (all tests):**
```java
@BeforeEach
public void setUp() throws Exception {
tempHome = Files.createTempDirectory("iq-test-serve-").toFile();
kernel = KernelBuilder.create()
.withHome(tempHome)
.build();
kernel.start();
context = new CLIContext(kernel);
}
```

**Example - Custom Port:**
```java
@Test
@DisplayName("ServeCommand custom API port")
public void testServeCustomApiPort() throws Exception {
ServeCommand serve = new ServeCommand(context);
serve.apiPort = 9000;
assertEquals(9000, serve.apiPort);
}
```

**Example - Error Handling:**
```java
@Test
@DisplayName("ServeCommand missing flags returns error")
public void testServeCommandMissingFlags() throws Exception {
ServeCommand serve = new ServeCommand(context);
Object result = serve.call();
assertTrue(result.toString().contains("error:no-servers-requested"));
}
```

---

## Usage Examples

### Development Workflow

**Start REST API only:**
```bash
$ iq serve --api
iq.serve: starting embedded servers
  host: 0.0.0.0
  starting REST API server on 0.0.0.0:8080

==================================================
IQ Embedded Servers Running
==================================================
  REST API:  http://0.0.0.0:8080

Press Ctrl-C to shutdown gracefully
==================================================

^Ciq.serve.shutdown: starting graceful shutdown...
iq.serve: shutdown initiated
```

**Start both servers with custom ports:**
```bash
$ iq serve --api --mcp --api-port 9000 --mcp-port 3001 --host 127.0.0.1
iq.serve: starting embedded servers
  host: 127.0.0.1
  starting REST API server on 127.0.0.1:9000
  starting MCP server on 127.0.0.1:3001

==================================================
IQ Embedded Servers Running
==================================================
  REST API:  http://127.0.0.1:9000
  MCP Server: 127.0.0.1:3001

Press Ctrl-C to shutdown gracefully
==================================================
```

**Error - Missing server flag:**
```bash
$ iq serve
iq.serve: missing --api or --mcp flag
  Usage: iq serve --api [--api-port PORT]
  Usage: iq serve --mcp [--mcp-port PORT]
  Usage: iq serve --api --mcp
error:no-servers-requested
```

---

## Integration Points

### Dependencies

| Component | Module | Purpose |
|---|---|---|
| `ServerRuntimeManager` | iq-platform | Server lifecycle management |
| `ServerRuntimeManagerFactory` | iq-platform | Runtime manager discovery |
| `AbstractCLICommand` | iq-cli | CLI command base class |
| `CLIContext` | iq-cli-pro | Kernel context and state |
| Picocli 4.x | External | Command-line parsing and execution |

### Kernel Context Access

```java
// Get context in command
KernelContext kc = context.getKernelContext();

// Available services
I_EventHub eventHub = kc.getEventHub();
I_Secrets secrets = kc.getSecrets();
File home = kc.getHome();
String version = kc.getVersion();
```

---

## Design Decisions

### Why ServerRuntimeManager?

Rather than directly launching Quarkus or MCP servers, ServeCommand delegates to `ServerRuntimeManager` to:
1. **Decouple from specific server implementations** — can swap Process, Quarkus, K8s modes
2. **Support process lifecycle management** — proper cleanup, PID tracking, health checks
3. **Enable multi-tenant deployments** — multiple servers per realm
4. **Reuse production patterns** — same manager used by admin APIs

### Why AtomicBoolean for running state?

- Thread-safe flag visible to shutdown hook
- Works with JVM shutdown hook (different thread context)
- Reliable cross-thread communication without locks

### Why block on synchronized(this)?

- Keep main thread alive while servers run
- Allows graceful notification from shutdown hook
- Prevents premature exit while servers are handling requests

---

## Future Enhancement

### Potential Features
1. Server health checks via `runtimeManager.health()` during runtime
2. Dynamic port assignment (port 0 → query actual port from OS)
3. Multi-realm support: `--realm urn:iq:realm:demo`
4. Configuration file support: `--config /etc/iq/servers.yaml`
5. Metrics export: `--metrics http://prometheus:9090`
6. Hot reload: `--watch` flag for code changes
7. Logging output streaming: `--logs` flag to see server output

### Integration with BootCommand

Once `BootCommand` is implemented, users could:
```bash
# Start agents first
iq boot --realm urn:iq:realm:default

# Then start API surface
iq serve --api --mcp

# Both share same kernel context
```

---

## File Locations

| File | Path | Purpose |
|---|---|---|
| Source | `iq-cli-pro/src/main/java/systems/symbol/cli/ServeCommand.java` | Command implementation |
| Tests | `iq-cli-pro/src/test/java/systems/symbol/cli/ServeCommandTest.java` | 25 unit tests |
| Registration | `iq-cli-pro/src/main/java/systems/symbol/cli/PowerCLI.java` | Command dispatcher |
| Documentation | `SERVE_IMPLEMENTATION.md` | This guide |

---

## Appendix: Maven Commands

**Run all tests:**
```bash
mvn -pl iq-cli-pro -am test
```

**Run ServeCommand tests only:**
```bash
mvn -pl iq-cli-pro -am test -Dtest=ServeCommandTest
```

**Compile without tests:**
```bash
mvn -pl iq-cli-pro -am compile
```

**Package CLI JAR:**
```bash
mvn -pl iq-cli-pro -am package
```

---

## See Also

- [TrustCommand Implementation](TRUST_IMPLEMENTATION.md) — PKI and OAuth flow
- [TriggerCommand Tests](iq-cli-pro/TODO.md) — Event routing and intent handling
- [ServerRuntimeManager](iq-platform) — Runtime lifecycle interface
- [PowerCLI](iq-cli-pro) — Command registration and dispatch
