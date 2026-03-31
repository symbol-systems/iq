package systems.symbol.mcp.client;

import jakarta.inject.Singleton;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Intent;
import systems.symbol.agent.I_IntentProvider;
import systems.symbol.agent.Intent;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCPClientIntentProvider — Bridge between remote MCP tools and IQ agent intents.
 *
 * <p>Implements {@link I_IntentProvider} to expose discovered remote MCP tools as
 * executable agent intents. This enables IQ agents to invoke remote tools seamlessly,
 * as if they were local capabilities.
 *
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>Load discovered remote tools from MCPClientRegistry cache</li>
 *   <li>Wrap each tool as an iq:Intent with name, description, and input schema</li>
 *   <li>On intent dispatch: delegate to the remote server via MCPClientRegistry</li>
 *   <li>Map remote tool results back to intent outcomes</li>
 * </ul>
 *
 * <p><b>Example Usage by Agent:</b>
 * <pre>
 *   // Agent discovers remote tool "web.search" as an intent
 *   I_Intent searchIntent = intentProvider.getIntent("iq:RemoteTool/web.search");
 *
 *   // Agent invokes the intent
 *   searchIntent.execute(Map.of("query", "climate change"));
 *   // Result contains output from the remote tool
 * </pre>
 *
 * <p><b>Configuration:</b>
 * Remote tools are made available as intents only if:
 * <ul>
 *   <li>The tool passes any "deniedToolPattern" regex filters</li>
 *   <li>The tool matches any "allowedToolPattern" regex filters</li>
 *   <li>The source server is enabled (mcp:enabled = true)</li>
 * </ul>
 */
@Singleton
public class MCPClientIntentProvider implements I_IntentProvider {

    private static final Logger log = LoggerFactory.getLogger(MCPClientIntentProvider.class);

    private final MCPClientRegistry clientRegistry;
    private final Repository repository;
    private final Map<String, I_Intent> intentCache = new ConcurrentHashMap<>();

    public MCPClientIntentProvider(MCPClientRegistry clientRegistry, Repository repository) {
        this.clientRegistry = clientRegistry;
        this.repository = repository;
    }

    /**
     * Get an intent by its identifier (IRI).
     *
     * <p>If the identifier matches the pattern "iq:RemoteTool/{serverName}/{toolName}",
     * the provider will fetch the corresponding remote tool descriptor and create
     * a local intent wrapper.
     *
     * @param intentId the intent IRI (e.g., "iq:RemoteTool/web.search")
     * @return an executable intent, or null if not found
     */
    @Override
    public I_Intent getIntent(String intentId) {
        if (intentId == null || !intentId.startsWith("iq:RemoteTool/")) {
            return null;
        }

        // Check cache first
        if (intentCache.containsKey(intentId)) {
            return intentCache.get(intentId);
        }

        // Parse tool identifier from IRI
        String toolPart = intentId.substring("iq:RemoteTool/".length());
        String[] parts = toolPart.split("/", 2);
        if (parts.length < 2) {
            log.warn("mcp.intent.parsing.failed: invalid format: {}", intentId);
            return null;
        }

        String serverName = parts[0];
        String toolName = parts[1];

        // Find the remote tool in cache
        List<MCPClientRegistry.RemoteTool> tools = clientRegistry.getCachedTools("default"); // TODO: extract realm from context
        MCPClientRegistry.RemoteTool remoteTool = tools.stream()
            .filter(t -> t.serverName.equals(serverName) && t.name.equals(toolName))
            .findFirst()
            .orElse(null);

        if (remoteTool == null) {
            log.debug("mcp.intent.tool.notfound: server={} tool={}", serverName, toolName);
            return null;
        }

        // Create intent wrapper
        I_Intent intent = createIntentForRemoteTool(remoteTool);
        intentCache.put(intentId, intent);
        return intent;
    }

    /**
     * List all available intents from remote tools.
     *
     * <p>Returns intents for all cached remote tools in the "default" realm.
     * TODO: Support per-realm intent lists.
     *
     * @return list of available remote tool intents
     */
    @Override
    public List<I_Intent> listIntents() {
        List<MCPClientRegistry.RemoteTool> tools = clientRegistry.getCachedTools("default");
        return tools.stream()
            .map(this::createIntentForRemoteTool)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific intent by name.
     *
     * <p>Resolves tool names like "web.search" to the corresponding intent.
     *
     * @param toolName the tool name (e.g., "web.search")
     * @return the intent, or null if not found
     */
    @Override
    public I_Intent getIntentByName(String toolName) {
        List<MCPClientRegistry.RemoteTool> tools = clientRegistry.getCachedTools("default");
        MCPClientRegistry.RemoteTool remoteTool = tools.stream()
            .filter(t -> t.name.equals(toolName))
            .findFirst()
            .orElse(null);

        return remoteTool != null ? createIntentForRemoteTool(remoteTool) : null;
    }

    /* ──────────────────────────────────────────────────────────────────────── */
    /* Factory Methods */
    /* ──────────────────────────────────────────────────────────────────────── */

    /**
     * Create an I_Intent wrapper for a remote MCP tool.
     *
     * @param remoteTool the remote tool descriptor
     * @return an executable intent
     */
    private I_Intent createIntentForRemoteTool(MCPClientRegistry.RemoteTool remoteTool) {
        Intent intent = new Intent();
        intent.setId("iq:RemoteTool/" + remoteTool.serverName + "/" + remoteTool.name);
        intent.setLabel(remoteTool.name);
        intent.setDescription(remoteTool.description);
        intent.setCategory("remote-tool");

        // Create an executor that delegates to the client registry
        intent.setExecutor((params) -> executeRemoteToolIntent(remoteTool, params));

        return intent;
    }

    /**
     * Execute a remote tool intent.
     *
     * <p>Maps intent parameters to tool input, invokes the remote tool,
     * and returns the result.
     *
     * @param remoteTool the remote tool
     * @param params intent parameters
     * @return execution result as a map
     */
    private Map<String, Object> executeRemoteToolIntent(MCPClientRegistry.RemoteTool remoteTool,
                                                        Map<String, Object> params) {
        try {
            // Extract principal and realm from context (TODO: from agent context)
            String principal = "system"; // Default principal
            String realm = "default";   // Default realm

            // Create MCP call context
            MCPCallContext ctx = new MCPCallContext(remoteTool.name, params);
            ctx.set(MCPCallContext.KEY_PRINCIPAL, principal);
            ctx.set(MCPCallContext.KEY_REALM, realm);

            log.info("mcp.intent.execute: tool={} server={} principal={} [trace={}]",
                    remoteTool.name, remoteTool.serverName, principal, ctx.traceId());

            // Invoke remote tool
            I_MCPResult result = clientRegistry.invokeRemoteTool(ctx, remoteTool, params);

            // Map result back to intent outcome
            Map<String, Object> outcome = new LinkedHashMap<>();
            outcome.put("status", "success");
            outcome.put("tool", remoteTool.name);
            outcome.put("result", result.getContent());
            outcome.put("mimeType", result.getMimeType());
            outcome.put("timestamp", System.currentTimeMillis());

            log.info("mcp.intent.execute.success: tool={} [trace={}]", remoteTool.name, ctx.traceId());

            return outcome;

        } catch (MCPException e) {
            log.warn("mcp.intent.execute.failed: tool={} -> {}", remoteTool.name, e.getMessage());

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("tool", remoteTool.name);
            error.put("error", e.getMessage());
            error.put("code", e.getCode());

            return error;

        } catch (Exception e) {
            log.error("mcp.intent.execute.exception: tool={}", remoteTool.name, e);

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("tool", remoteTool.name);
            error.put("error", "Unexpected failure: " + e.getMessage());
            error.put("code", 500);

            return error;
        }
    }

    /**
     * Load all remote tool intents from the cache.
     *
     * <p>Called during agent startup or realm activation to make remote tools available.
     *
     * @param realm the realm identifier
     */
    public void loadRemoteToolsForRealm(String realm) {
        log.info("mcp.intent.load: realm={}", realm);

        List<MCPClientRegistry.RemoteTool> tools = clientRegistry.getCachedTools(realm);
        int count = 0;

        for (MCPClientRegistry.RemoteTool tool : tools) {
            try {
                String intentId = "iq:RemoteTool/" + tool.serverName + "/" + tool.name;
                I_Intent intent = createIntentForRemoteTool(tool);
                intentCache.put(intentId, intent);
                count++;

                log.debug("mcp.intent.loaded: {} from {}", tool.name, tool.serverName);
            } catch (Exception e) {
                log.warn("mcp.intent.load.failed: tool={} -> {}", tool.name, e.getMessage());
            }
        }

        log.info("mcp.intent.load.complete: realm={} count={}", realm, count);
    }

    /**
     * Refresh the intent cache by discovering tools from all enabled servers.
     *
     * <p>Called periodically or on-demand to update tool availability.
     *
     * @param realm the realm identifier
     */
    public void refreshRemoteTools(String realm) {
        log.info("mcp.intent.refresh: realm={}", realm);
        intentCache.clear();
        clientRegistry.discoverServers(realm);
        loadRemoteToolsForRealm(realm);
    }

    /**
     * Check if an intent is a remote tool intent.
     *
     * @param intentId the intent identifier
     * @return true if this is a remote tool intent
     */
    public boolean isRemoteToolIntent(String intentId) {
        return intentId != null && intentId.startsWith("iq:RemoteTool/");
    }

    /**
     * Get tool metadata from the RDF cache.
     *
     * <p>Retrieves full tool descriptor including input/output schemas and examples.
     *
     * @param realm the realm
     * @param toolName the tool name
     * @return tool metadata, or null if not found
     */
    public Map<String, Object> getToolMetadata(String realm, String toolName) {
        try (RepositoryConnection conn = repository.getConnection()) {
            String query = """
                PREFIX mcp: <urn:mcp:>
                SELECT ?schema ?output ?examples
                FROM NAMED <urn:mcp:cache:tools>
                WHERE {
                    ?tool mcp:toolName \"""" + toolName + """" ;
                          mcp:toolInputSchema ?schema .
                    OPTIONAL { ?tool mcp:toolOutputSchema ?output . }
                    OPTIONAL { ?tool mcp:toolExamples ?examples . }
                }
                LIMIT 1
                """;

            try (var result = conn.prepareTupleQuery(query).evaluate()) {
                if (result.hasNext()) {
                    var bs = result.next();
                    Map<String, Object> metadata = new HashMap<>();
                    if (bs.getValue("schema") != null) {
                        metadata.put("inputSchema", parseJson(bs.getValue("schema").stringValue()));
                    }
                    if (bs.getValue("output") != null) {
                        metadata.put("outputSchema", parseJson(bs.getValue("output").stringValue()));
                    }
                    if (bs.getValue("examples") != null) {
                        metadata.put("examples", parseJson(bs.getValue("examples").stringValue()));
                    }
                    return metadata;
                }
            }
        } catch (Exception e) {
            log.warn("mcp.intent.metadata.failed: tool={}", toolName, e);
        }

        return null;
    }

    /* ──────────────────────────────────────────────────────────────────────── */
    /* Utility Methods */
    /* ──────────────────────────────────────────────────────────────────────── */

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("mcp.intent.json.parse.failed: {}", e.getMessage());
            return null;
        }
    }
}
