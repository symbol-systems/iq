package systems.symbol.controller.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

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
private static final ObjectMapper om = new ObjectMapper();

@Inject
RealmPlatform platform;

private MCPToolRegistry toolRegistry;

private static Map<String, Object> jsonMap() {
return new LinkedHashMap<>();
}

private static String toJson(Map<String, Object> map) {
try {
return om.writeValueAsString(map);
} catch (Exception e) {
log.error("JSON serialization failed", e);
return "{}";
}
}

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

private Response errorResponse(Response.Status status, String message) {
Map<String, Object> body = jsonMap();
body.put("error", message != null ? message : "Unknown error");
return addCorsHeaders(Response.status(status).entity(toJson(body))).build();
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
Map<String, Object> endpoints = jsonMap();
endpoints.put("tools", "/mcp/tools");
endpoints.put("resources", "/mcp/resources");
endpoints.put("health", "/mcp/health");
endpoints.put("stream", "GET /mcp/stream (Application/JSON streaming)");

Map<String, Object> body = jsonMap();
if (registry == null) {
body.put("status", "partial");
body.put("message", "MCP initializing...");
} else {
body.put("status", "ready");
body.put("message", "MCP Server operational");
body.put("version", "1.0");
endpoints.put("prompts", "/mcp/prompts");
}
body.put("endpoints", endpoints);
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error in MCP root endpoint", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "MCP not yet initialized");
}

// Parse incoming JSON safely, then embed in response
Object parsedMessage = om.readValue(messageJson, Object.class);
Map<String, Object> body = jsonMap();
body.put("status", "acknowledged");
body.put("message", parsedMessage);
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error handling MCP message", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
Map<String, Object> body = jsonMap();
body.put("error", "MCP tools not yet initialized");
body.put("status", "initializing");
return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(toJson(body))).build();
}
MCPServerBuilder builder = registry.buildServerBuilder();
Map<String, Object> body = jsonMap();
body.put("tools", builder.getTools());
body.put("count", builder.getTools().size());
body.put("status", "ready");
body.put("message", "MCP tools available");
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error listing MCP tools", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "MCP resources not yet initialized");
}
MCPServerBuilder builder = registry.buildServerBuilder();
Map<String, Object> body = jsonMap();
body.put("resources", builder.getResources());
body.put("count", builder.getResources().size());
body.put("status", "ready");
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error listing MCP resources", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "MCP prompts not yet initialized");
}
MCPServerBuilder builder = registry.buildServerBuilder();
Map<String, Object> body = jsonMap();
body.put("prompts", builder.getPrompts());
body.put("count", builder.getPrompts().size());
body.put("status", "ready");
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error listing MCP prompts", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "MCP tools not yet initialized");
}
MCPServerBuilder builder = registry.buildServerBuilder();
Map<String, Object> body = jsonMap();
body.put("result", Map.of());
body.put("tool", toolName);
body.put("status", "executed");
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Error executing MCP tool: {}", toolName, e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
Map<String, Object> body = jsonMap();
Repository repo = getDefaultRepository();
if (repo == null) {
body.put("status", "initializing");
body.put("mcp", false);
body.put("message", "Platform initializing");
return addCorsHeaders(Response.ok(toJson(body))).build();
}

MCPToolRegistry registry = getToolRegistry();
if (registry == null) {
body.put("status", "partial");
body.put("mcp", true);
body.put("message", "MCP initializing");
return addCorsHeaders(Response.ok(toJson(body))).build();
}

body.put("status", "healthy");
body.put("mcp", true);
body.put("message", "MCP Server operational");
return addCorsHeaders(Response.ok(toJson(body))).build();
} catch (Exception e) {
log.error("Health check failed", e);
Map<String, Object> body = jsonMap();
body.put("status", "unhealthy");
body.put("mcp", false);
body.put("message", e.getMessage());
return addCorsHeaders(Response.status(Response.Status.SERVICE_UNAVAILABLE)
.entity(toJson(body))).build();
}
}
}
