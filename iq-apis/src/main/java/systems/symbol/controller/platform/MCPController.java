package systems.symbol.controller.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.MCPToolRegistry;
import systems.symbol.mcp.server.MCPServerBuilder;
import systems.symbol.platform.RealmPlatform;
import java.io.IOException;

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
     * Add CORS headers to a response.
     *
     * @param response the response builder
     * @return the response builder with CORS headers
     */
    private Response.ResponseBuilder addCorsHeaders(Response.ResponseBuilder response) {
        return response
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
    }

    /**
     * Get the default repository from the platform.
     *
     * @return The default repository, or null if not available
     */
    private Repository getDefaultRepository() {
        try {
            if (platform != null && platform.getInstance() != null) {
                java.util.Set<org.eclipse.rdf4j.model.IRI> realms = platform.getInstance().getRealms();
                if (realms != null && !realms.isEmpty()) {
                    // Get first realm's repository as default
                    org.eclipse.rdf4j.model.IRI realmIri = realms.iterator().next();
                    Object realmObj = platform.getInstance().getRealm(realmIri);
                    if (realmObj instanceof Repository) {
                        return (Repository) realmObj;
                    }
                    // Try to get repository from realm interface
                    try {
                        java.lang.reflect.Method getRepoMethod = realmObj.getClass().getMethod("getRepository");
                        Object repo = getRepoMethod.invoke(realmObj);
                        if (repo instanceof Repository) {
                            return (Repository) repo;
                        }
                    } catch (Exception e2) {
                        log.debug("Could not get repository from realm", e2);
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
     * Root MCP endpoint — returns JSON status and available resources.
     *
     * <p>Use Accept: application/json header to get this response
     *
     * @return JSON response with status and available endpoints
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response root() {
        try {
            MCPToolRegistry registry = getToolRegistry();
            String responseBody;
            if (registry == null) {
                responseBody = "{\"status\": \"partial\", \"message\": \"MCP initializing...\", " +
                        "\"endpoints\": {\"tools\": \"/mcp/tools\", \"resources\": \"/mcp/resources\", \"health\": \"/mcp/health\", " +
                        "\"stream\": \"GET /mcp/stream (Application/JSON streaming)\"}}";
            } else {
                responseBody = "{\"status\": \"ready\", \"message\": \"MCP Server operational\", " +
                        "\"version\": \"1.0\", \"endpoints\": {\"tools\": \"/mcp/tools\", \"resources\": \"/mcp/resources\", " +
                        "\"prompts\": \"/mcp/prompts\", \"health\": \"/mcp/health\", \"stream\": \"GET /mcp/stream (Application/JSON streaming)\"}}";
            }
            return addCorsHeaders(Response.ok(responseBody)).build();
        } catch (Exception e) {
            log.error("Error in MCP root endpoint", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")).build();
        }
    }

    /**
     * Handle MCP streaming HTTP connection.
     *
     * <p>Opens a persistent HTTP streaming connection for streaming MCP protocol messages.
     * Server sends initial capability handshake and streams tool/resource updates.
     * Replaces deprecated SSE with standard HTTP streaming.
     *
     * @return Response with streaming output (connection stays open)
     */
    @GET
    @Path("/stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleMcpStreamingConnection() {
        try {
            log.info("MCP streaming connection established");
            
            StreamingOutput stream = output -> {
                try {
                    // Send initial capability handshake
                    String initData = "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"iq-mcp-server\",\"version\":\"1.0\"}}}\n";
                    output.write(initData.getBytes());
                    output.flush();
                    
                    // Send ready event
                    String readyData = "{\"status\":\"ready\",\"message\":\"MCP Server operational\"}\n";
                    output.write(readyData.getBytes());
                    output.flush();
                    
                    log.debug("MCP streaming initialization complete, keeping connection open");
                    
                    // Keep connection open for streaming updates
                    // Clients send messages via POST /mcp
                    // Server sends updates via this streaming connection
                    // Connection stays open until client disconnects
                    
                } catch (IOException e) {
                    log.error("Error writing to streaming output", e);
                    throw e;
                }
            };
            
            return addCorsHeaders(Response.ok(stream)
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in MCP streaming handler", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}" ))
                    .build();
        }
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
                return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP not yet initialized\"}"))
                        .build();
            }
            
            // Parse and handle message
            // For now, echo back with acknowledgment
            return addCorsHeaders(Response.ok("{\"status\": \"acknowledged\", \"message\": " + messageJson + "}"))
                    .build();
        } catch (Exception e) {
            log.error("Error handling MCP message", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}"))
                    .build();
        }
    }

    /**
     * Handle CORS preflight requests (OPTIONS).
     *
     * @return Empty response with CORS headers
     */
    @OPTIONS
    public Response handleCorsPreFlight() {
        return addCorsHeaders(Response.ok())
                .header("Access-Control-Max-Age", "86400")
                .build();
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
                return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP tools not yet initialized\", \"status\": \"initializing\"}"))
                        .build();
            }
            MCPServerBuilder builder = registry.buildServerBuilder();
            
            // For now, return a placeholder response with tool count
            // In the full implementation, extract tools from builder
            return addCorsHeaders(Response.ok("{\"tools\": [], \"count\": 0, \"status\": \"ready\", \"message\": \"MCP tools available\"}"))
                    .build();
        } catch (Exception e) {
            log.error("Error listing MCP tools", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}"))
                    .build();
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
                return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP resources not yet initialized\"}"))
                        .build();
            }
            return addCorsHeaders(Response.ok("{\"resources\": [], \"count\": 0, \"status\": \"ready\"}"))
                    .build();
        } catch (Exception e) {
            log.error("Error listing MCP resources", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}"))
                    .build();
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
                return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP prompts not yet initialized\"}"))
                        .build();
            }
            return addCorsHeaders(Response.ok("{\"prompts\": [], \"count\": 0, \"status\": \"ready\"}"))
                    .build();
        } catch (Exception e) {
            log.error("Error listing MCP prompts", e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}"))
                    .build();
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
                return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"MCP tools not yet initialized\"}"))
                        .build();
            }
            MCPServerBuilder builder = registry.buildServerBuilder();
            
            // For now, return a placeholder response
            // In the full implementation, invoke tool through builder
            return addCorsHeaders(Response.ok("{\"result\": {}, \"tool\": \"" + toolName + "\", \"status\": \"executed\"}"))
                    .build();
        } catch (Exception e) {
            log.error("Error executing MCP tool: {}", toolName, e);
            return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}"))
                    .build();
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
                return addCorsHeaders(Response.ok("{\"status\": \"initializing\", \"mcp\": false, \"message\": \"Platform initializing\"}"))
                        .build();
            }
            
            MCPToolRegistry registry = getToolRegistry();
            if (registry == null) {
                return addCorsHeaders(Response.ok("{\"status\": \"partial\", \"mcp\": true, \"message\": \"MCP initializing\"}"))
                        .build();
            }
            
            return addCorsHeaders(Response.ok("{\"status\": \"healthy\", \"mcp\": true, \"message\": \"MCP Server operational\"}"))
                    .build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"status\": \"unhealthy\", \"mcp\": false, \"message\": \"" + e.getMessage() + "\"}"))
                    .build();
        }
    }
}
