package systems.symbol.mcp.server;

import io.modelcontextprotocol.server.McpSyncServer;
import systems.symbol.mcp.I_MCPPrompt;
import systems.symbol.mcp.I_MCPResource;
import systems.symbol.mcp.I_MCPTool;

import java.util.List;
import java.util.Map;

/**
 * MCPServerBuilder — anti-corruption layer between IQ SPIs and Anthropic MCP SDK.
 *
 * <p>Converts {@link I_MCPTool}, {@link I_MCPResource}, and {@link I_MCPPrompt}
 * implementations to SDK-compatible handlers. This class imports MCP SDK types;
 * all IQ logic stays in SPI interfaces.
 *
 * <p>Configuration (server name, version, middleware stack) is externalized
 * to {@code mcp-server-config.ttl} and loaded via RDF bootstrap. The builder
 * is a simple holder for server metadata and adapters.
 *
 * <p><b>SDK 1.1.0 Support:</b>
 * The builder now works with MCP SDK 1.1.0, which includes:
 * <ul>
 *   <li>Synchronous and asynchronous server APIs via {@code McpSyncServer}</li>
 *   <li>Built-in support for stdio and SSE transports</li>
 *   <li>Jackson 2.17.0+ for JSON serialization</li>
 *   <li>Full RPC method binding for tools, resources, and prompts</li>
 * </ul>
 *
 * <p><b>Transport Setup:</b>
 * Actual {@code McpSyncServer} construction and transport configuration are deferred
 * to Quarkus bootstrap via CDI producers. The builder outputs metadata; transport
 * initialization happens in the Quarkus runtime (for stdio, SSE, or Vert.x adapters).
 *
 * <p><b>Example Usage:</b>
 * <pre>
 *   var builder = new MCPServerBuilder()
 *       .withMetadata("iq-mcp", "1.0", Map.of())
 *       .tools(sparqlQueryTool, sparqlUpdateTool)
 *       .resources(namespaceResource)
 *       .prompts(List.of());
 *
 *   // In Quarkus CDI producer:
 *   // McpSyncServer server = ServerFactory.create(builder);
 *   // StdioServerTransport transport = new StdioServerTransport(server);
 *   // transport.start();
 * </pre>
 */
public class MCPServerBuilder {

    private String serverName    = null;  // loaded from config
    private String serverVersion = null;  // loaded from config

    private List<I_MCPTool>     tools     = List.of();
    private List<I_MCPResource> resources = List.of();
    private List<I_MCPPrompt>   prompts   = List.of();

    private Map<String, Object> serverMetadata = Map.of();

    /* ── configuration injection (from TTL) ─────────────────────────────── */

    public MCPServerBuilder withMetadata(String name, String version, Map<String, Object> meta) {
        this.serverName = name;
        this.serverVersion = version;
        this.serverMetadata = meta;
        return this;
    }

    /* ── fluent adapter registration ──────────────────────────────────────── */

    public MCPServerBuilder tools(List<I_MCPTool> tools) {
        this.tools = (tools != null) ? tools : List.of();
        return this;
    }

    public MCPServerBuilder resources(List<I_MCPResource> resources) {
        this.resources = (resources != null) ? resources : List.of();
        return this;
    }

    public MCPServerBuilder prompts(List<I_MCPPrompt> prompts) {
        this.prompts = (prompts != null) ? prompts : List.of();
        return this;
    }

    /* ── accessors (for SDK integration) ───────────────────────────────────── */

    public String getServerName()    { return serverName; }
    public String getServerVersion() { return serverVersion; }
    public List<I_MCPTool> getTools() { return tools; }
    public List<I_MCPResource> getResources() { return resources; }
    public List<I_MCPPrompt> getPrompts() { return prompts; }
    public Map<String, Object> getServerMetadata() { return serverMetadata; }

    /**
     * Placeholder for SDK 1.1.0 integration.
     * 
     * <p>Actual server construction is deferred to Quarkus bootstrap where
     * transports (stdio, SSE, or HTTP) are configured based on deployment context.
     * 
     * <p>Future: Return a configured {@code McpSyncServer} once Quarkus
     * CDI producer wiring is in place.
     * 
     * @return null (transports configured at runtime)
     * @see <a href="https://github.com/modelcontextprotocol/sdk-java">MCP SDK Java</a>
     */
    public McpSyncServer build() {
        // SDK 1.1.0: Transport setup deferred to Quarkus runtime
        // Server creation (with tool/resource/prompt bindings) happens in CDI producer
        return null;
    }
}
