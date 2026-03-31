package systems.symbol.controller.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.MCPToolRegistry;
import systems.symbol.mcp.server.MCPServerBuilder;
import systems.symbol.platform.RealmPlatform;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST Controller for MCP (Model Context Protocol) endpoints.
 *
 * <p>Exposes IQ's knowledge graph and workflows as MCP tools that any LLM can use.
 *
 * <p>Available endpoints:
 * <ul>
 *   <li>GET /mcp — Root endpoint with status and available resources</li>
 *   <li>GET /mcp/tools — List all available MCP tools</li>
 *   <li>POST /mcp/tools/{name}/execute — Execute a tool</li>
 *   <li>GET /mcp/resources — List available knowledge graph resources</li>
 *   <li>GET /mcp/prompts — List available prompt templates</li>
 *   <li>GET /mcp/health — Health check</li>
 * </ul>
 */
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
public class MCPController {
    private static final Logger log = LoggerFactory.getLogger(MCPController.class);

    @Inject
    RealmPlatform platform;

    private MCPToolRegistry toolRegistry;

    /**
     * Get the default repository from the platform.
     *
     * @return The default repository, or null if not available
     */
    private Repository getDefaultRepository() {
        try {
            if (platform != null && platform.getInstance() != null) {
                java.util.Set<String> realms = platform.getInstance().getRealms();
                if (realms != null && !realms.isEmpty()) {
                    // Get first realm's repository as default
                    systems.symbol.realm.Realm realm = platform.getInstance().getRealm(realms.iterator().next());
                    if (realm != null) {
                        return realm.getRepository();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get repository from platform", e);
        }
        return null;
    }

    /**
     * Initialize the MCP tool registry on first use (lazy initialization).
     */
    private synchronized MCPToolRegistry getToolRegistry() {
        if (toolRegistry == null) {
            Repository repo = getDefaultRepository();
            if (repo != null) {
                toolRegistry = new MCPToolRegistry(repo);
                log.info("MCP Tool Registry initialized");
            } else {
                log.warn("Repository not available for MCP initialization");
            }
        }
        return toolRegistry;
    }

    /**
     * Root MCP endpoint — supports both JSON REST and SSE streaming.
     *
     * <p>GET with Accept: application/json returns JSON status  
     * GET with Accept: text/event-stream opens SSE connection for MCP protocol  
     * POST accepts MCP client messages
     *
     * @param sseEventSink SSE sink for streaming (injected if SSE requested)
     * @param sse SSE context (injected if SSE requested)
     * @return Response (JSON or SSE stream depending on Accept header)
     */
    @GET
    public Response root(@Context(required = false) SseEventSink sseEventSink, 
                         @Context(required = false) Sse sse) {
        try {
            // If SSE connection requested, open stream
            if (sseEventSink != null && sse != null) {
                return handleMcpSseConnection(sseEventSink, sse);
            }
            
            // Otherwise return JSON status
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.ok("{\"status\": \"partial\", \"message\": \"MCP initializing...\", " +
                        "\"endpoints\": {\"tools\": \"/mcp/tools\", \"resources\": \"/mcp/resources\", \"health\": \"/mcp/health\", " +
                        "\"sse\": \"GET (Accept: text/event-stream)\"}}").build();
            }
            return Response.ok("{\"status\": \"ready\", \"message\": \"MCP Server operational\", " +
                    "\"version\": \"1.0\", \"endpoints\": {\"tools\": \"/mcp/tools\", \"resources\": \"/mcp/resources\", " +
                    "\"prompts\": \"/mcp/prompts\", \"health\": \"/mcp/health\", \"sse\": \"GET (Accept: text/event-stream)\"}}").build();
        } catch (Exception e) {
            log.error("Error in MCP root endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Handle MCP SSE (Server-Sent Events) connection.
     *
     * <p>Opens a persistent SSE connection for streaming MCP protocol messages.
     * Server sends initial capability handshake and streams tool/resource updates.
     *
     * @param sseEventSink the SSE event sink for sending events
     * @param sse the SSE context
     * @return Response with appropriate status
     */
    private Response handleMcpSseConnection(SseEventSink sseEventSink, Sse sse) {
        try {
            log.info("MCP SSE connection established");
            
            // Send initial capability handshake
            OutboundSseEvent event = sse.newEvent("initialization", 
                    "{\"protocol\": \"model context protocol\", \"version\": \"1.0\", " +
                    "\"capabilities\": {\"tools\": true, \"resources\": true, \"prompts\": true}}");
            event.setId(String.valueOf(System.currentTimeMillis()));
            sseEventSink.send(event);
            
            // Send ready event
            OutboundSseEvent readyEvent = sse.newEvent("ready",
                    "{\"status\": \"ready\", \"message\": \"MCP Server operational\"}");
            readyEvent.setId(String.valueOf(System.currentTimeMillis() + 1));
            sseEventSink.send(readyEvent);
            
            log.debug("MCP SSE initialization complete, keeping connection open");
            
            // Connection stays open for bidirectional communication
            // Client can send messages via POST /mcp
            // Server sends updates via this SSE stream
            
        } catch (Exception e) {
            log.error("Error in MCP SSE handler", e);
            try {
                sseEventSink.close();
            } catch (Exception closeEx) {
                log.warn("Error closing SSE sink", closeEx);
            }
        }
        
        return Response.ok().build();
    }

    /**
     * Accept MCP client messages via POST.
     *
     * <p>Handles messages from MCP clients (LLMs, tools) sent to the server.
     *
     * @param messageJson the client message in JSON format
     * @return Response with server's response message
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleMcpMessage(String messageJson) {
        try {
            log.debug("Received MCP message: {}", messageJson);
            
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP not yet initialized\"}").build();
            }
            
            // Parse and handle message
            // For now, echo back with acknowledgment
            return Response.ok("{\"status\": \"acknowledged\", \"message\": " + messageJson + "}").build();
        } catch (Exception e) {
            log.error("Error handling MCP message", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * List all available MCP tools.
     *
     * @return JSON array of tool definitions
     */
    @GET
    @Path("/tools")
    public Response listTools() {
        try {
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP tools not yet initialized\", \"status\": \"initializing\"}").build();
            }
            MCPServerBuilder builder = registry.buildServerBuilder();
            
            // For now, return a placeholder response with tool count
            // In the full implementation, extract tools from builder
            return Response.ok("{\"tools\": [], \"count\": 0, \"status\": \"ready\", \"message\": \"MCP tools available\"}").build();
        } catch (Exception e) {
            log.error("Error listing MCP tools", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * List all available resources (knowledge graph entities).
     *
     * @return JSON array of resource definitions
     */
    @GET
    @Path("/resources")
    public Response listResources() {
        try {
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP resources not yet initialized\"}").build();
            }
            return Response.ok("{\"resources\": [], \"count\": 0, \"status\": \"ready\"}").build();
        } catch (Exception e) {
            log.error("Error listing MCP resources", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * List all available prompts.
     *
     * @return JSON array of prompt definitions
     */
    @GET
    @Path("/prompts")
    public Response listPrompts() {
        try {
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP prompts not yet initialized\"}").build();
            }
            return Response.ok("{\"prompts\": [], \"count\": 0, \"status\": \"ready\"}").build();
        } catch (Exception e) {
            log.error("Error listing MCP prompts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Execute a named MCP tool with parameters.
     *
     * @param toolName the name of the tool to execute
     * @param params the tool parameters (JSON)
     * @return result from the tool
     */
    @POST
    @Path("/tools/{name}/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeTool(@PathParam("name") String toolName, String params) {
        try {
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP tools not yet initialized\"}").build();
            }
            MCPServerBuilder builder = registry.buildServerBuilder();
            
            // For now, return a placeholder response
            // In the full implementation, invoke tool through builder
            return Response.ok("{\"result\": {}, \"tool\": \"" + toolName + "\", \"status\": \"executed\"}").build();
        } catch (Exception e) {
            log.error("Error executing MCP tool: {}", toolName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Health check endpoint.
     *
     * @return health status
     */
    @GET
    @Path("/health")
    public Response health() {
        try {
            Repository repo = getDefaultRepository();
            if (repo == null) {
                return Response.ok("{\"status\": \"initializing\", \"mcp\": false, \"message\": \"Platform initializing\"}").build();
            }
            
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return Response.ok("{\"status\": \"partial\", \"mcp\": true, \"message\": \"MCP initializing\"}").build();
            }
            
            return Response.ok("{\"status\": \"healthy\", \"mcp\": true, \"message\": \"MCP Server operational\"}").build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"status\": \"degraded\", \"mcp\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }
}
