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
 */
public final class MCPServerMain {

    private static final Logger log = LoggerFactory.getLogger(MCPServerMain.class);

    private MCPServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String repoName = System.getenv("MY_IQ");
        if (repoName == null || repoName.isBlank()) {
            repoName = "iq";
        }

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
        String mcpBanner = "\n" +
                "╔═════════════════════════════════════════════╗\n" +
                "║ IQ MCP Server (symbol.systems)              ║\n" +
                "║ version: " + version + "\n" +
                "║ repo: " + repoName + "\n" +
                "║ java: " + System.getProperty("java.version") + "\n" +
                "╚═════════════════════════════════════════════╝\n";
        System.out.print(mcpBanner);
        log.info("iq.mcp.started: repo={}", repoName);
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
