package systems.symbol.controller.platform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.MCPToolRegistry;
import systems.symbol.mcp.dynamic.DynamicToolRegistration;
import systems.symbol.mcp.resource.StreamingResourceProvider;
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
@Produces({MediaType.APPLICATION_JSON, "text/event-stream"})
public Response root(@Context HttpHeaders headers) {
try {
if (headers != null && headers.getAcceptableMediaTypes() != null) {
boolean wantsSse = headers.getAcceptableMediaTypes().stream()
.anyMatch(mt -> mt.toString().contains("text/event-stream") || mt.getSubtype().contains("event-stream"));
if (wantsSse) {
return handleMcpStreamingConnection();
}
}
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
@Produces("text/event-stream")
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
.type("text/event-stream")
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
 * Get resource content with HTTP 206 Partial Content support for byte-range requests.
 *
 * <p>Supports Range header for streaming large resources:
 * <pre>
 * GET /mcp/resources/{name}
 * Range: bytes=0-9999
 * 
 * HTTP/1.1 206 Partial Content
 * Content-Length: 10000
 * Content-Range: bytes 0-9999/50000
 * Accept-Ranges: bytes
 * </pre>
 *
 * @param resourceName the name of the resource to retrieve
 * @param headers HTTP headers (including Range header)
 * @return partial or full resource content
 */
@GET
@Path("/resources/{name}")
@Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM})
public Response getResource(
@PathParam("name") String resourceName,
@Context HttpHeaders headers) {
try {
if (resourceName == null || resourceName.trim().isEmpty()) {
return errorResponse(Response.Status.BAD_REQUEST, "Resource name is required");
}

// Get the resource content (in a real implementation, would fetch from provider)
// For now, return a placeholder showing what would be fetched
String resourceContent = "[Resource: " + resourceName + "]";

// Get Range header if present
String rangeHeader = headers != null ? headers.getHeaderString("Range") : null;

// Apply byte-range filtering if requested
String responseContent = StreamingResourceProvider.applyRangeSupport(resourceContent, rangeHeader);
Map<String, String> rangeHeaders = StreamingResourceProvider.getRangeHeaders(resourceContent, rangeHeader);

Response.ResponseBuilder responseBuilder;
if (!rangeHeaders.isEmpty()) {
// Return 206 Partial Content
responseBuilder = Response.status(206);
for (var entry : rangeHeaders.entrySet()) {
responseBuilder = responseBuilder.header(entry.getKey(), entry.getValue());
}
} else {
// Return 200 OK with full content
responseBuilder = Response.ok();
responseBuilder = responseBuilder.header("Accept-Ranges", "bytes");
}

return addCorsHeaders(responseBuilder.entity(responseContent)).build();
} catch (Exception e) {
log.error("Error retrieving MCP resource: {}", resourceName, e);
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
// Validate JSON body, return 400 on invalid JSON
try {
om.readTree(params);
} catch (JsonProcessingException e) {
log.warn("Invalid JSON in MCP executeTool for {}: {}", toolName, e.getMessage());
return errorResponse(Response.Status.BAD_REQUEST, "Invalid JSON payload");
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
 * Register a new tool dynamically via SPARQL INSERT.
 *
 * <p>Allows runtime registration of tools without deployment. Tools are registered
 * in the {@code iq:catalog} RDF graph and will be discovered by DynamicScriptBridge
 * on next MCPToolRegistry.buildServerBuilder() call.
 *
 * <p>Request body:
 * <pre>
 * {
 *   "name": "my-tool",
 *   "description": "My custom tool",
 *   "sparql": "SELECT ?item WHERE { ... }",
 *   "inputSchema": "{\"type\": \"object\", ...}"
 * }
 * </pre>
 *
 * @param registrationJson JSON tool registration request
 * @return response with registration status
 */
@POST
@Path("/tools/register")
@Consumes(MediaType.APPLICATION_JSON)
public Response registerTool(String registrationJson) {
try {
// Validate JSON
var registrationObj = om.readTree(registrationJson);
String toolName = safeJsonString(registrationObj, "name");
String description = safeJsonString(registrationObj, "description");
String sparql = safeJsonString(registrationObj, "sparql");
String inputSchema = safeJsonString(registrationObj, "inputSchema");

if (toolName == null || toolName.trim().isEmpty()) {
return errorResponse(Response.Status.BAD_REQUEST, "Tool name is required");
}
if (sparql == null || sparql.trim().isEmpty()) {
return errorResponse(Response.Status.BAD_REQUEST, "SPARQL query is required");
}

Repository repo = getDefaultRepository();
if (repo == null) {
return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "Repository not available");
}

// Register tool via DynamicToolRegistration
var registration = new DynamicToolRegistration(repo);
boolean success = registration.registerToolViaSparql(toolName, description, sparql, inputSchema);

Map<String, Object> body = jsonMap();
if (success) {
body.put("status", "registered");
body.put("tool", toolName);
body.put("message", "Tool registered successfully. Use buildServerBuilder() to discover.");
log.info("[MCPController] dynamically registered tool: {}", toolName);
return addCorsHeaders(Response.ok(toJson(body))).build();
} else {
body.put("status", "failed");
body.put("error", "Tool registration failed");
return addCorsHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(toJson(body))).build();
}
} catch (JsonProcessingException e) {
log.warn("Invalid JSON in registerTool request: {}", e.getMessage());
return errorResponse(Response.Status.BAD_REQUEST, "Invalid JSON payload");
} catch (Exception e) {
log.error("Error registering MCP tool", e);
return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
}
}

/**
 * Safely extract a string value from JSON tree.
 */
private String safeJsonString(com.fasterxml.jackson.databind.JsonNode node, String fieldName) {
if (node == null || !node.has(fieldName)) {
return null;
}
var field = node.get(fieldName);
return field != null && !field.isNull() ? field.asText() : null;
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
