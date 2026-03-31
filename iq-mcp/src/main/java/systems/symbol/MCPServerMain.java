package systems.symbol;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.MCPToolRegistry;
import systems.symbol.mcp.server.MCPServerBuilder;

/**
 * Launches the IQ MCP server over stdio.
 *
 * @deprecated Use HTTP(S) endpoints via MCPController in iq-apis instead.
 *             Run: bin/iq-mcp or bin/iq-cli-server server mcp start
 *             MCP tools are exposed as REST endpoints at http://localhost:8080/mcp/*
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public final class MCPServerMain {

    private static final Logger log = LoggerFactory.getLogger(MCPServerMain.class);

    private MCPServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String repoName = System.getenv("MY_IQ");
        if (repoName == null || repoName.isBlank()) {
            repoName = "iq";
        }

        // Force standard JDK logging manager, as JBoss logmanager service descriptor can produce
        // "Failed to instantiate LoggerFinder provider" on some JVMs when malformed service declarations are present.
        System.setProperty("java.util.logging.manager", "java.util.logging.LogManager");

        // Optional safe fallback for SLF4J when no binding is found; will be overridden by a binding if present.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        Repository repository = new SailRepository(new MemoryStore());
        repository.init();

        MCPServerBuilder builder = new MCPToolRegistry(repository).buildServerBuilder();
        var server = builder.build();
        if (server == null) {
            throw new IllegalStateException("Unable to start MCP server");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.closeGracefully();
            }
            catch (Exception e) {
                log.warn("mcp.shutdown.error: {}", e.getMessage());
            }
            try {
                repository.shutDown();
            }
            catch (Exception e) {
                log.warn("mcp.repository.shutdown.error: {}", e.getMessage());
            }
        }, "iq-mcp-shutdown"));

        String version = "unknown";
        try {
            version = systems.symbol.platform.I_Self.version();
        } catch (Exception e) {
            // ignore
        }

        var current = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
        var userHome = System.getProperty("user.home", "unknown");
        var javaVendor = System.getProperty("java.vendor", "unknown");
        var loggingManager = System.getProperty("java.util.logging.manager", "unknown");

        String mcpBanner = "\n" +
                "║ version: " + version + "\n" +
                "║ realm: " + repoName + "\n" +
                "║ transport: stdio (MCP protocol over stdin/stdout)\n" +
                "║ endpoint URL: n/a (stdio mode)\n" +
                "║ java: " + System.getProperty("java.version") + " (" + javaVendor + ")\n" +
                "║ logging_manager: " + loggingManager + "\n" +
                "║ working_dir: " + current + "\n" +
                "║ home_dir: " + userHome + "\n" +
                "║ configured_tools: " + builder.getTools().size() + "\n" +
                "║ configured_resources: " + builder.getResources().size() + "\n" +
                "║ configured_prompts: " + builder.getPrompts().size() + "\n" +
                "\n";
        System.out.print(mcpBanner);
        log.info("iq.mcp.started: repo={}, version={}, tools={}, resources={}, prompts={}",
                repoName, version, builder.getTools().size(), builder.getResources().size(), builder.getPrompts().size());
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
