package systems.symbol.mcp.client;

import jakarta.inject.Singleton;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * MCPClientRegistry — Remote MCP server discovery and tool caching.
 *
 * <p>Manages connections to remote MCP servers, discovers available tools,
 * and caches them in named RDF graphs for agent access. Enables IQ agents
 * to invoke remote tools as if they were local.
 *
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>Discover MCPServer instances from RDF model (realm.ttl or system config)</li>
 *   <li>For each enabled server: fetch tool/resource/prompt lists via HTTP</li>
 *   <li>Cache discovered items in named graphs (urn:mcp:cache:tools, etc.)</li>
 *   <li>Validate cache TTL and refresh as needed</li>
 *   <li>Provide lookup methods for agents to discover and invoke remote tools</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 *   MCPClientRegistry registry = /* injected */;
 *   registry.discoverServers("default");
 *   List&lt;RemoteTool&gt; tools = registry.getCachedTools("default");
 *   I_MCPResult result = registry.invokeRemoteTool(ctx, "tool-name", input);
 * </pre>
 */
@Singleton
public class MCPClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(MCPClientRegistry.class);

    private static final String CACHE_GRAPH_TOOLS      = "urn:mcp:cache:tools";
    private static final String CACHE_GRAPH_RESOURCES  = "urn:mcp:cache:resources";
    private static final String CACHE_GRAPH_PROMPTS    = "urn:mcp:cache:prompts";

    private final Repository repository;
    private final HttpClient httpClient;
    private final Map<String, ServerConnection> connectionCache;

    public MCPClientRegistry(Repository repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.connectionCache = new HashMap<>();
    }

    /**
     * Discover all enabled MCP servers for a realm and fetch their tools.
     *
     * @param realm the realm identifier
     * @return count of discovered servers
     */
    public int discoverServers(String realm) {
        log.info("mcp.client.discover: realm={}", realm);

        int count = 0;
        try (RepositoryConnection conn = repository.getConnection()) {
            // Load all mcp:MCPServer instances from the model
            String query = """
                PREFIX mcp: <urn:mcp:>
                SELECT ?server ?name ?transport ?uri ?command ?auth ?token ?cacheTTL ?enabled
                WHERE {
                    ?server a mcp:MCPServer .
                    ?server mcp:name ?name .
                    ?server mcp:transport ?transport .
                    OPTIONAL { ?server mcp:serverUri ?uri . }
                    OPTIONAL { ?server mcp:command ?command . }
                    OPTIONAL { ?server mcp:authScheme ?auth . }
                    OPTIONAL { ?server mcp:bearerToken ?token . }
                    OPTIONAL { ?server mcp:cacheTTL ?cacheTTL . }
                    OPTIONAL { ?server mcp:enabled ?enabled . }
                    FILTER (!bound(?enabled) || ?enabled = true)
                }
                """;

            try (var result = conn.prepareTupleQuery(query).evaluate()) {
                while (result.hasNext()) {
                    var bs = result.next();
                    String serverName = bs.getValue("name").stringValue();
                    String transport = bs.getValue("transport").stringValue();

                    log.debug("mcp.client.discover: server={} transport={}", serverName, transport);

                    ServerConfig config = parseServerConfig(bs);
                    try {
                        cacheToolsFromServer(realm, config, conn);
                        count++;
                    } catch (Exception e) {
                        log.warn("mcp.client.discover.failed: server={} -> {}", serverName, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("mcp.client.discover.error: realm={}", realm, e);
        }

        log.info("mcp.client.discover.complete: realm={} servers={}", realm, count);
        return count;
    }

    /**
     * Fetch tools from a remote server and cache them.
     *
     * @param realm the realm identifier
     * @param config server configuration
     * @param conn repository connection
     * @throws Exception on fetch or cache failure
     */
    public void cacheToolsFromServer(String realm, ServerConfig config, RepositoryConnection conn) throws Exception {
        log.info("mcp.client.cache.start: server={} transport={}", config.name, config.transport);

        if ("http".equalsIgnoreCase(config.transport) || "sse".equalsIgnoreCase(config.transport)) {
            cacheToolsViaHttp(realm, config, conn);
        } else if ("stdio".equalsIgnoreCase(config.transport)) {
            log.warn("mcp.client.cache.stdio.notimpl: server={}", config.name);
            // Stdio transport requires process spawning — not implemented in Phase 3
        } else {
            log.warn("mcp.client.cache.unknown.transport: server={} transport={}", config.name, config.transport);
        }
    }

    /**
     * Fetch tools from a remote HTTP(S) MCP server.
     *
     * @param realm the realm identifier
     * @param config server configuration
     * @param conn repository connection
     * @throws Exception on HTTP or parsing failure
     */
    private void cacheToolsViaHttp(String realm, ServerConfig config, RepositoryConnection conn) throws Exception {
        if (config.serverUri == null) {
            throw new IllegalArgumentException("HTTP transport requires serverUri");
        }

        // Build authorization header
        String authHeader = config.authScheme != null ? buildAuthHeader(config) : null;

        // Fetch tool list from /mcp/tools
        String toolsUrl = config.serverUri + "/tools";
        Map<String, Object> toolsResponse = fetchJson(toolsUrl, authHeader);

        if (toolsResponse != null && toolsResponse.containsKey("tools")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) toolsResponse.get("tools");

            for (Map<String, Object> tool : tools) {
                String toolName = (String) tool.get("name");
                String description = (String) tool.get("description");
                Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");

                // Store tool in cache graph
                storeDiscoveredTool(realm, config.name, toolName, description, inputSchema, conn);
                log.debug("mcp.client.tool.cached: server={} tool={}", config.name, toolName);
            }
        }
    }

    /**
     * Store a discovered tool in the RDF cache graph.
     *
     * @param realm the realm identifier
     * @param serverName server name
     * @param toolName tool name
     * @param description tool description
     * @param inputSchema JSON Schema object
     * @param conn repository connection
     */
    private void storeDiscoveredTool(String realm, String serverName, String toolName,
                                     String description, Map<String, Object> inputSchema,
                                     RepositoryConnection conn) {
        IRI cacheGraph = Values.iri(CACHE_GRAPH_TOOLS);
        IRI toolIri = Values.iri("urn:mcp:tool:" + realm + ":" + serverName + ":" + toolName);

        // Triple 1: Type declaration
        conn.add(toolIri, Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                 Values.iri("urn:mcp:DiscoveredTool"), cacheGraph);

        // Triple 2: Tool name
        Literal nameValue = conn.getValueFactory().createLiteral(toolName);
        conn.add(toolIri, Values.iri("urn:mcp:toolName"), nameValue, cacheGraph);

        // Triple 3: Description (optional)
        if (description != null) {
            Literal descValue = conn.getValueFactory().createLiteral(description);
            conn.add(toolIri, Values.iri("urn:mcp:toolDescription"), descValue, cacheGraph);
        }

        // Triple 4: Input schema (JSON string)
        if (inputSchema != null) {
            String schemaJson = toJson(inputSchema);
            Literal schemaValue = conn.getValueFactory().createLiteral(schemaJson);
            conn.add(toolIri, Values.iri("urn:mcp:toolInputSchema"), schemaValue, cacheGraph);
        }

        // Triple 5: Cached timestamp
        Literal now = conn.getValueFactory().createLiteral(Instant.now());
        conn.add(toolIri, Values.iri("urn:mcp:cachedAt"), now, cacheGraph);

        // Triple 6: Server reference
        IRI serverRef = Values.iri("urn:mcp:server:" + serverName);
        conn.add(toolIri, Values.iri("urn:mcp:sourceServer"), serverRef, cacheGraph);
    }

    /**
     * Get all cached tools for a realm.
     *
     * @param realm the realm identifier
     * @return list of cached remote tools
     */
    public List<RemoteTool> getCachedTools(String realm) {
        List<RemoteTool> tools = new ArrayList<>();

        try (RepositoryConnection conn = repository.getConnection()) {
            String query = """
                PREFIX mcp: <urn:mcp:>
                SELECT ?tool ?name ?description ?server
                FROM NAMED <urn:mcp:cache:tools>
                WHERE {
                    ?tool a mcp:DiscoveredTool ;
                          mcp:toolName ?name ;
                          mcp:sourceServer ?server .
                    OPTIONAL { ?tool mcp:toolDescription ?description . }
                }
                ORDER BY ?name
                """;

            try (var result = conn.prepareTupleQuery(query).evaluate()) {
                while (result.hasNext()) {
                    var bs = result.next();
                    RemoteTool tool = new RemoteTool(
                        bs.getValue("name").stringValue(),
                        bs.getValue("description") != null ? bs.getValue("description").stringValue() : "",
                        bs.getValue("tool").toString(),
                        bs.getValue("server").toString()
                    );
                    tools.add(tool);
                }
            }
        } catch (Exception e) {
            log.error("mcp.client.getcached.error: realm={}", realm, e);
        }

        return tools;
    }

    /**
     * Invoke a remote tool on a specific server.
     *
     * @param ctx call context with principal and realm
     * @param remoteTool the remote tool to invoke
     * @param input tool input parameters
     * @return execution result
     * @throws MCPException on execution failure
     */
    public I_MCPResult invokeRemoteTool(MCPCallContext ctx, RemoteTool remoteTool,
                                        Map<String, Object> input) throws MCPException {
        log.info("mcp.client.invoke.start: tool={} server={} [trace={}]",
                remoteTool.name, remoteTool.serverName, ctx.traceId());

        try {
            ServerConnection conn = getOrCreateConnection(remoteTool.serverName);

            // Build request to remote server's /mcp/tools/{name}/execute endpoint
            String executeUrl = conn.config.serverUri + "/tools/" + remoteTool.name + "/execute";
            String authHeader = buildAuthHeader(conn.config);

            Map<String, Object> response = postJsonWithAuth(executeUrl, input, authHeader);

            if (response != null && "success".equals(response.get("status"))) {
                Object result = response.get("result");
                String mimeType = (String) response.getOrDefault("result_mimeType", "application/json");
                return MCPResult.ok(toJson(result), mimeType);
            } else {
                String errorMsg = response != null ? response.toString() : "Unknown error";
                throw MCPException.internal("Remote tool failed: " + errorMsg, null);
            }
        } catch (MCPException e) {
            throw e;
        } catch (Exception e) {
            log.error("mcp.client.invoke.failed: tool={} [trace={}]", remoteTool.name, ctx.traceId(), e);
            throw MCPException.internal("Remote tool invocation failed: " + e.getMessage(), e);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────── */
    /* Helper Methods */
    /* ──────────────────────────────────────────────────────────────────────── */

    private ServerConnection getOrCreateConnection(String serverName) {
        return connectionCache.computeIfAbsent(serverName, k -> {
            try (RepositoryConnection conn = repository.getConnection()) {
                String query = """
                    PREFIX mcp: <urn:mcp:>
                    SELECT ?uri ?transport ?auth ?token ?timeout
                    WHERE {
                        ?server mcp:name \"""" + serverName + """" ;
                                mcp:serverUri ?uri ;
                                mcp:transport ?transport .
                        OPTIONAL { ?server mcp:authScheme ?auth . }
                        OPTIONAL { ?server mcp:bearerToken ?token . }
                        OPTIONAL { ?server mcp:httpTimeout ?timeout . }
                    }
                    LIMIT 1
                    """;

                try (var result = conn.prepareTupleQuery(query).evaluate()) {
                    if (result.hasNext()) {
                        var bs = result.next();
                        ServerConfig config = new ServerConfig();
                        config.name = serverName;
                        config.serverUri = bs.getValue("uri").stringValue();
                        config.transport = bs.getValue("transport").stringValue();
                        if (bs.getValue("auth") != null) {
                            config.authScheme = bs.getValue("auth").stringValue();
                        }
                        if (bs.getValue("token") != null) {
                            config.bearerToken = bs.getValue("token").stringValue();
                        }
                        config.httpTimeout = bs.getValue("timeout") != null ?
                            Integer.parseInt(bs.getValue("timeout").stringValue()) : 10000;

                        return new ServerConnection(config);
                    }
                }
            } catch (Exception e) {
                log.warn("mcp.client.connection.loadfailed: server={}", serverName, e);
            }
            return new ServerConnection(new ServerConfig());
        });
    }

    private ServerConfig parseServerConfig(org.eclipse.rdf4j.query.BindingSet bs) {
        ServerConfig config = new ServerConfig();
        config.name = bs.getValue("name").stringValue();
        config.transport = bs.getValue("transport").stringValue();
        if (bs.getValue("uri") != null) config.serverUri = bs.getValue("uri").stringValue();
        if (bs.getValue("command") != null) config.command = bs.getValue("command").stringValue();
        if (bs.getValue("auth") != null) config.authScheme = bs.getValue("auth").stringValue();
        if (bs.getValue("token") != null) config.bearerToken = bs.getValue("token").stringValue();
        if (bs.getValue("cacheTTL") != null) config.cacheTTL = Integer.parseInt(bs.getValue("cacheTTL").stringValue());
        return config;
    }

    private String buildAuthHeader(ServerConfig config) {
        if ("bearer".equalsIgnoreCase(config.authScheme) && config.bearerToken != null) {
            return "Bearer " + config.bearerToken;
        } else if ("api-key".equalsIgnoreCase(config.authScheme) && config.bearerToken != null) {
            return "X-API-Key: " + config.bearerToken;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchJson(String url, String authHeader) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder().uri(URI.create(url)).GET();
        if (authHeader != null) {
            req.header("Authorization", authHeader);
        }

        HttpResponse<String> response = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return parseJson(response.body());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJsonWithAuth(String url, Object body, String authHeader)
            throws IOException, InterruptedException {
        String bodyJson = toJson(body);
        HttpRequest.Builder req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

        if (authHeader != null) {
            req.header("Authorization", authHeader);
        }

        HttpResponse<String> response = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
        return parseJson(response.body());
    }

    private String toJson(Object obj) {
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            boolean first = true;
            for (var e : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(e.getKey()).append("\":");
                sb.append(toJson(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            return sb.append("]").toString();
        }
        return "\"" + String.valueOf(obj).replace("\"", "\\\"") + "\"";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // Simple JSON parsing — replace with Jackson if available
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("mcp.client.json.parse.failed: {}", e.getMessage());
            return null;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────── */
    /* Inner Classes */
    /* ──────────────────────────────────────────────────────────────────────── */

    public static class ServerConfig {
        public String name;
        public String transport;
        public String serverUri;
        public String command;
        public String authScheme;
        public String bearerToken;
        public int cacheTTL = -1; // -1 = permanent
        public int httpTimeout = 10000;
    }

    private static class ServerConnection {
        final ServerConfig config;

        ServerConnection(ServerConfig config) {
            this.config = config;
        }
    }

    public static class RemoteTool {
        public final String name;
        public final String description;
        public final String toolIri;
        public final String serverName;

        public RemoteTool(String name, String description, String toolIri, String serverName) {
            this.name = name;
            this.description = description;
            this.toolIri = toolIri;
            this.serverName = serverName;
        }

        @Override
        public String toString() {
            return "RemoteTool{" + name + " from " + serverName + "}";
        }
    }
}
