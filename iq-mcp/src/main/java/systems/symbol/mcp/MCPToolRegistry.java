package systems.symbol.mcp;

import systems.symbol.mcp.connect.MCPConnectPipeline;
import systems.symbol.mcp.connect.MCPConnectRegistry;
import systems.symbol.mcp.dynamic.DynamicAgentBridge;
import systems.symbol.mcp.dynamic.DynamicScriptBridge;
import systems.symbol.mcp.server.MCPServerBuilder;
import systems.symbol.mcp.tool.ActorTriggerAdapter;
import systems.symbol.mcp.tool.RdfDescribeAdapter;
import systems.symbol.mcp.tool.SparqlQueryAdapter;
import systems.symbol.mcp.tool.SparqlUpdateAdapter;
import systems.symbol.mcp.resource.NamespacesResourceProvider;
import systems.symbol.mcp.resource.SchemaResourceProvider;
import systems.symbol.mcp.resource.VoidResourceProvider;
import org.eclipse.rdf4j.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCPToolRegistry — central wiring point for the IQ MCP server.
 *
 * <p>Aggregates all static and dynamic tools, resources, and prompts,
 * then produces a fully-configured {@link MCPServerBuilder} ready to build
 * an MCP SDK {@code McpSyncServer}.
 *
 * <p>Configuration (server name, version, middleware) is externalized to
 * {@code mcp-server-config.ttl} and loaded during Quarkus CDI bootstrap.
 */
public class MCPToolRegistry {

    private final Repository repository;

    private DynamicScriptBridge.ScriptRunner      scriptRunner;
    private DynamicAgentBridge.AgentTransitionRunner agentRunner;
    private ActorTriggerAdapter.I_IntentDispatcher  intentDispatcher;

    private String serverName    = null;  // from config
    private String serverVersion = null;  // from config
    private Map<String, Object> metadata = Map.of();

    public MCPToolRegistry(Repository repository) {
        this.repository = repository;
    }

    /* ── fluent configuration ────────────────────────────────────────────── */

    public MCPToolRegistry withDynamicScripts(DynamicScriptBridge.ScriptRunner r) {
        this.scriptRunner = r;
        return this;
    }

    public MCPToolRegistry withDynamicAgents(DynamicAgentBridge.AgentTransitionRunner r) {
        this.agentRunner = r;
        return this;
    }

    public MCPToolRegistry withIntentDispatcher(ActorTriggerAdapter.I_IntentDispatcher d) {
        this.intentDispatcher = d;
        return this;
    }

    public MCPToolRegistry withServerMetadata(String name, String version, Map<String, Object> meta) {
        this.serverName = name;
        this.serverVersion = version;
        this.metadata = (meta != null) ? meta : Map.of();
        return this;
    }

    /**
     * Build the fully-configured {@link MCPServerBuilder}.
     */
    public MCPServerBuilder buildServerBuilder() {
        /* ── middleware pipeline ── */
        var middlewares  = new MCPConnectRegistry(repository).loadAll();
        var pipeline     = new MCPConnectPipeline(middlewares);

        /* ── static tools (4 pillars) ── */
        List<I_MCPTool> tools = new ArrayList<>();
        tools.add(new SparqlQueryAdapter(repository));
        tools.add(new SparqlUpdateAdapter(repository));
        tools.add(new RdfDescribeAdapter(repository));
        tools.add(new systems.symbol.mcp.tool.ServerMgmtTool());

        // Pillar C: actor trigger (agentic bridge)
        ActorTriggerAdapter actorAdapter = intentDispatcher != null
                ? new ActorTriggerAdapter(intentDispatcher)
                : ActorTriggerAdapter.noOp();
        tools.add(actorAdapter);

        /* ── dynamic script tools (Phase 3.1) ── */
        if (scriptRunner != null) {
            var scriptBridge = new DynamicScriptBridge(repository, scriptRunner);
            List<I_MCPTool> scriptTools = scriptBridge.discover();
            tools.addAll(scriptTools);
        }

        /* ── dynamic agent tools (Phase 3.2) ── */
        if (agentRunner != null) {
            var agentBridge = new DynamicAgentBridge(repository, agentRunner);
            List<I_MCPTool> agentTools = agentBridge.discover();
            tools.addAll(agentTools);
        }

        /* ── resources (Pillar D) ── */
        List<I_MCPResource> resources = List.of(
            new NamespacesResourceProvider(repository),
            new SchemaResourceProvider(repository),
            new VoidResourceProvider(repository)
        );

        /* ── prompts ── */
        List<I_MCPPrompt> prompts = List.of(
            new systems.symbol.mcp.prompt.ComposeQueryPrompt()
        );

        return new MCPServerBuilder()
                .withMetadata(serverName, serverVersion, metadata)
                .tools(tools)
                .resources(resources)
                .prompts(prompts);
    }
}
