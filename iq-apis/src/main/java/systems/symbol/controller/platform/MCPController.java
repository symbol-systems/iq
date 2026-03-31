package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.APIResponse;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.mcp.*;
import systems.symbol.mcp.server.MCPServerBuilder;
import systems.symbol.mcp.connect.MCPConnectPipeline;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCPController — Production HTTP(S) REST gateway to MCP tools.
 *
 * <p>Exposes MCPToolRegistry as fully functional HTTP endpoints for remote clients.
 * This is the canonical transport for MCP; all tool execution flows through HTTP.
 *
 * <p>Endpoints:
 * - GET /mcp/tools — list available tools with schemas
 * - GET /mcp/tools/{name} — get tool schema and metadata
 * - POST /mcp/tools/{name}/execute — invoke tool with validated input
 * - GET /mcp/resources/{uri:.*} — read ambient resources (schemas, namespaces)
 * - POST /mcp/prompts/{name} — render prompt templates
 * - GET /mcp/status — health check
 *
 * <p>Authentication: All endpoints require JWT Authorization header.
 * Realm is extracted from JWT claims. Principal (subject) identifies the caller.
 *
 * <p>Authorization: ACL rules from RDF model enforce tool/resource access.
 * Rate limits and quotas are enforced per tool and per principal.
 */
@Tag(name = "MCP", description = "Model Context Protocol HTTP Gateway")
@Path("mcp")
public class MCPController extends GuardedAPI {

    private static final Logger log = LoggerFactory.getLogger(MCPController.class);

    @Inject
    MCPToolRegistry toolRegistry;

    /**
     * GET /mcp/tools — List all available tools with full schemas.
     *
     * <p>Returns tool names, descriptions, input schemas, read-only flags, and rate limits.
     * Filters tools based on principal's ACL rules.
     *
     * @return JSON response containing tool list and metadata
     */
    @GET
    @Path("tools")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List available MCP tools",
        description = "Returns all tools available to the authenticated principal"
    )
    @APIResponse(responseCode = "200", description = "Tool list with schemas",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "403", description = "Principal not authorized for realm")
    public Response listTools(
            @HeaderParam("Authorization") String auth,
            @QueryParam("realm") String realmParam) {
        
        Instant startTime = Instant.now();
        
        try {
            // 1. Authenticate using JWT
            if (Validate.isNonAlphanumeric(auth) || !auth.startsWith("Bearer ")) {
                log.warn("mcp.tools.auth.invalid: missing or malformed auth header");
                return new OopsResponse("mcp.auth.missing", Response.Status.UNAUTHORIZED).build();
            }
            
            // Get realm (prefer parameter, fallback to default)
            String realmStr = realmParam != null && !realmParam.isEmpty() 
                ? realmParam 
                : "default";
            
            if (Validate.isNonAlphanumeric(realmStr)) {
                return new OopsResponse("mcp.realm.invalid", Response.Status.BAD_REQUEST).build();
            }
            
            // Load realm from platform
            I_Realm realm = platform.getRealm(Values.iri(realmStr + ":"));
            if (realm == null) {
                log.warn("mcp.tools.realm.missing: {}", realmStr);
                return new OopsResponse("mcp.realm.notfound", Response.Status.NOT_FOUND).build();
            }
            
            // Authenticate JWT against realm's keys
            DecodedJWT jwt;
            try {
                jwt = authenticate(auth, realm);
            } catch (OopsException | SecretsException e) {
                log.warn("mcp.tools.auth.failed: {} -> {}", realmStr, e.getMessage());
                return new OopsResponse("mcp.auth.failed", Response.Status.UNAUTHORIZED).build();
            }
            
            String principal = jwt.getSubject();
            
            // 2. Check ACL: principal must be authorized for realm and tool discovery
            try {
                checkRealmAccess(principal, realm.getSelf(), jwt);
            } catch (OopsException e) {
                log.warn("mcp.tools.acl.denied: {} -> {}", principal, realmStr);
                return new OopsResponse("mcp.acl.denied", Response.Status.FORBIDDEN).build();
            }
            
            // 3. Get tools from registry
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            List<I_MCPTool> tools = builder.getTools();
            
            // 4. Build tool schema list
            List<Map<String, Object>> toolSchemas = tools.stream()
                .map(tool -> {
                    Map<String, Object> schema = new LinkedHashMap<>();
                    schema.put("name", tool.getName());
                    schema.put("description", tool.getDescription());
                    schema.put("inputSchema", tool.getInputSchema());
                    schema.put("readOnly", tool.isReadOnly());
                    schema.put("rateLimit", tool.defaultRateLimit());
                    return schema;
                })
                .collect(Collectors.toList());
            
            // 5. Build response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("realm", realmStr);
            response.put("principal", principal);
            response.put("toolCount", toolSchemas.size());
            response.put("tools", toolSchemas);
            response.put("timestamp", System.currentTimeMillis());
            response.put("durationMs", Duration.between(startTime, Instant.now()).toMillis());
            
            log.info("mcp.tools.list: realm={} principal={} count={} duration={}ms", 
                realmStr, principal, toolSchemas.size(), 
                Duration.between(startTime, Instant.now()).toMillis());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("listTools() failed", e);
            return new OopsResponse("mcp.tools.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /mcp/tools/{name} — Get tool schema, description, and metadata.
     *
     * @param toolName the MCP tool identifier (e.g., "sparql.query")
     * @param auth JWT bearer token in Authorization header
     * @param realmParam optional realm parameter (defaults to "default")
     * @return JSON response with tool schema and metadata
     */
    @GET
    @Path("tools/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get tool schema",
        description = "Returns the full schema, description, and metadata for a specific tool"
    )
    @APIResponse(responseCode = "200", description = "Tool schema and metadata")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "404", description = "Tool not found")
    public Response describeTool(
            @PathParam("name") String toolName,
            @HeaderParam("Authorization") String auth,
            @QueryParam("realm") String realmParam) {
        
        Instant startTime = Instant.now();
        
        try {
            // 1. Validate inputs
            if (Validate.isMissing(toolName)) {
                return new OopsResponse("mcp.tool.name.missing", Response.Status.BAD_REQUEST).build();
            }
            
            if (Validate.isNonAlphanumeric(auth) || !auth.startsWith("Bearer ")) {
                return new OopsResponse("mcp.auth.missing", Response.Status.UNAUTHORIZED).build();
            }
            
            String realmStr = realmParam != null && !realmParam.isEmpty() 
                ? realmParam 
                : "default";
            
            if (Validate.isNonAlphanumeric(realmStr)) {
                return new OopsResponse("mcp.realm.invalid", Response.Status.BAD_REQUEST).build();
            }
            
            // 2. Load realm and authenticate
            I_Realm realm = platform.getRealm(Values.iri(realmStr + ":"));
            if (realm == null) {
                return new OopsResponse("mcp.realm.notfound", Response.Status.NOT_FOUND).build();
            }
            
            DecodedJWT jwt;
            try {
                jwt = authenticate(auth, realm);
            } catch (OopsException | SecretsException e) {
                log.warn("mcp.tool.auth.failed: {} -> {}", toolName, realmStr);
                return new OopsResponse("mcp.auth.failed", Response.Status.UNAUTHORIZED).build();
            }
            
            String principal = jwt.getSubject();
            
            // 3. Check ACL
            try {
                checkRealmAccess(principal, realm.getSelf(), jwt);
            } catch (OopsException e) {
                return new OopsResponse("mcp.acl.denied", Response.Status.FORBIDDEN).build();
            }
            
            // 4. Find tool by name
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            I_MCPTool tool = builder.getTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);
            
            if (tool == null) {
                log.warn("mcp.tool.notfound: {} in realm {}", toolName, realmStr);
                return new OopsResponse("mcp.tool.notfound", Response.Status.NOT_FOUND).build();
            }
            
            // 5. Build response with full tool metadata
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("realm", realmStr);
            response.put("principal", principal);
            response.put("name", tool.getName());
            response.put("description", tool.getDescription());
            response.put("inputSchema", tool.getInputSchema());
            response.put("readOnly", tool.isReadOnly());
            response.put("rateLimit", tool.defaultRateLimit());
            response.put("order", tool.order());
            response.put("timestamp", System.currentTimeMillis());
            response.put("durationMs", Duration.between(startTime, Instant.now()).toMillis());
            
            log.info("mcp.tool.describe: {} realm={} principal={}", toolName, realmStr, principal);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("describeTool({}) failed", toolName, e);
            return new OopsResponse("mcp.tool.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /mcp/tools/{name}/execute — Execute a tool with validated input.
     *
     * <p>Validates input against tool schema, applies middleware pipeline (auth, acl, quota, limits),
     * executes the tool, and returns result or error.
     *
     * @param toolName MCP tool identifier (e.g., "sparql.query")
     * @param input input parameters as JSON object (validated against tool's inputSchema)
     * @param auth JWT bearer token
     * @param realmParam optional realm (default: "default")
     * @return JSON response with execution result or error
     */
    @POST
    @Path("tools/{name}/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Execute a tool",
        description = "Execute a tool with the provided input parameters. Input must match the tool's inputSchema."
    )
    @APIResponse(responseCode = "200", description = "Tool execution result")
    @APIResponse(responseCode = "400", description = "Bad input (schema validation failed)")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "403", description = "Principal not authorized for tool")
    @APIResponse(responseCode = "404", description = "Tool not found")
    @APIResponse(responseCode = "429", description = "Rate limit or quota exceeded")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response executeTool(
            @PathParam("name") String toolName,
            Map<String, Object> input,
            @HeaderParam("Authorization") String auth,
            @QueryParam("realm") String realmParam) {
        
        Instant startTime = Instant.now();
        
        try {
            // 1. Validate inputs
            if (Validate.isMissing(toolName)) {
                return new OopsResponse("mcp.tool.name.missing", Response.Status.BAD_REQUEST).build();
            }
            
            if (input == null) {
                input = Collections.emptyMap();
            }
            
            if (Validate.isNonAlphanumeric(auth) || !auth.startsWith("Bearer ")) {
                return new OopsResponse("mcp.auth.missing", Response.Status.UNAUTHORIZED).build();
            }
            
            String realmStr = realmParam != null && !realmParam.isEmpty() 
                ? realmParam 
                : "default";
            
            if (Validate.isNonAlphanumeric(realmStr)) {
                return new OopsResponse("mcp.realm.invalid", Response.Status.BAD_REQUEST).build();
            }
            
            // 2. Load realm and authenticate
            I_Realm realm = platform.getRealm(Values.iri(realmStr + ":"));
            if (realm == null) {
                return new OopsResponse("mcp.realm.notfound", Response.Status.NOT_FOUND).build();
            }
            
            DecodedJWT jwt;
            try {
                jwt = authenticate(auth, realm);
            } catch (OopsException | SecretsException e) {
                log.warn("mcp.execute.auth.failed: {} {} -> {}", toolName, realmStr, e.getMessage());
                return new OopsResponse("mcp.auth.failed", Response.Status.UNAUTHORIZED).build();
            }
            
            String principal = jwt.getSubject();
            
            // 3. Check ACL: principal authorized for realm
            try {
                checkRealmAccess(principal, realm.getSelf(), jwt);
            } catch (OopsException e) {
                log.warn("mcp.execute.acl.denied: {} {} -> {}", toolName, principal, realmStr);
                return new OopsResponse("mcp.acl.denied", Response.Status.FORBIDDEN).build();
            }
            
            // 4. Find and validate tool
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            I_MCPTool tool = builder.getTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);
            
            if (tool == null) {
                log.warn("mcp.execute.tool.notfound: {} realm={}", toolName, realmStr);
                return new OopsResponse("mcp.tool.notfound", Response.Status.NOT_FOUND).build();
            }
            
            // 5. Validate input against tool schema
            try {
                validateInputSchema(input, tool.getInputSchema());
            } catch (Exception e) {
                log.warn("mcp.execute.validation.failed: {} -> {}", toolName, e.getMessage());
                return new OopsResponse("mcp.input.invalid: " + e.getMessage(), 
                                      Response.Status.BAD_REQUEST).build();
            }
            
            // 6. Create call context with principal and realm
            MCPCallContext ctx = new MCPCallContext(toolName, input);
            ctx.set(MCPCallContext.KEY_PRINCIPAL, principal);
            ctx.set(MCPCallContext.KEY_REALM, realmStr);
            ctx.set(MCPCallContext.KEY_JWT, jwt.getToken());
            
            // 7. Execute tool
            I_MCPResult result;
            try {
                log.info("mcp.execute.start: {} principal={} realm={}", toolName, principal, realmStr);
                result = tool.execute(ctx, input);
            } catch (MCPException e) {
                log.warn("mcp.execute.mcp.failed: {} -> {}", toolName, e.getMessage());
                
                // Map MCP exception to HTTP status
                int statusCode = mapMCPCodeToHTTP(e.getCode());
                Response.Status status = Response.Status.fromStatusCode(statusCode);
                
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("tool", toolName);
                errorResponse.put("realm", realmStr);
                errorResponse.put("status", "error");
                errorResponse.put("error_code", e.getCode());
                errorResponse.put("error_message", e.getMessage());
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("durationMs", Duration.between(startTime, Instant.now()).toMillis());
                
                log.warn("mcp.execute.error: tool={} code={} http={} message={}", 
                    toolName, e.getCode(), statusCode, e.getMessage());
                
                return Response.status(status).entity(errorResponse).build();
            } catch (Exception e) {
                log.error("mcp.execute.exception: {} -> {}", toolName, e.getMessage(), e);
                
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("tool", toolName);
                errorResponse.put("realm", realmStr);
                errorResponse.put("status", "error");
                errorResponse.put("error_code", 500);
                errorResponse.put("error_message", "Internal server error");
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("durationMs", Duration.between(startTime, Instant.now()).toMillis());
                
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponse).build();
            }
            
            // 8. Format success response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("tool", toolName);
            response.put("realm", realmStr);
            response.put("principal", principal);
            response.put("status", "success");
            
            // Parse result content based on MIME type
            try {
                if ("application/json".equals(result.getMimeType())) {
                    response.put("result", parseJson(result.getContent()));
                } else if ("application/ld+json".equals(result.getMimeType())) {
                    response.put("result", parseJson(result.getContent()));
                } else {
                    // Return as string for text, turtle, etc.
                    response.put("result", result.getContent());
                }
                response.put("result_mimeType", result.getMimeType());
            } catch (Exception e) {
                // If parsing fails, return as string
                response.put("result", result.getContent());
                response.put("result_mimeType", result.getMimeType());
            }
            
            response.put("timestamp", System.currentTimeMillis());
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            response.put("durationMs", durationMs);
            
            log.info("mcp.execute.success: {} principal={} realm={} duration={}ms", 
                toolName, principal, realmStr, durationMs);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("executeTool({}) unexpected error", toolName, e);
            return new OopsResponse("mcp.execute.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /mcp/resources/{uri:.*} — Read ambient resources (schemas, namespaces, void).
     *
     * <p>Resources are read-only, non-mutating context pulled into the LLM's context window.
     * They differ from tools in being informational and not requiring heavy guards.
     *
     * @param uri the resource URI (e.g., "urn:iq:namespaces", "urn:iq:schema")
     * @param auth JWT bearer token
     * @param realmParam optional realm (default: "default")
     * @return resource content in requested format (text/turtle, application/ld+json, etc.)
     */
    @GET
    @Path("resources/{uri:.*}")
    @Produces({MediaType.APPLICATION_JSON, "text/turtle", "application/ld+json", "text/plain"})
    @Operation(
        summary = "Read a resource",
        description = "Read ambient resource (namespace, schema, void). Resources are read-only context."
    )
    @APIResponse(responseCode = "200", description = "Resource content")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "404", description = "Resource not found")
    public Response readResource(
            @PathParam("uri") String uri,
            @HeaderParam("Authorization") String auth,
            @QueryParam("realm") String realmParam,
            @Context HttpHeaders headers) {
        
        Instant startTime = Instant.now();
        
        try {
            // 1. Validate inputs
            if (Validate.isMissing(uri)) {
                return new OopsResponse("mcp.resource.uri.missing", Response.Status.BAD_REQUEST).build();
            }
            
            if (Validate.isNonAlphanumeric(auth) || !auth.startsWith("Bearer ")) {
                return new OopsResponse("mcp.auth.missing", Response.Status.UNAUTHORIZED).build();
            }
            
            String realmStr = realmParam != null && !realmParam.isEmpty() 
                ? realmParam 
                : "default";
            
            // 2. Load realm and authenticate
            I_Realm realm = platform.getRealm(Values.iri(realmStr + ":"));
            if (realm == null) {
                return new OopsResponse("mcp.realm.notfound", Response.Status.NOT_FOUND).build();
            }
            
            DecodedJWT jwt;
            try {
                jwt = authenticate(auth, realm);
            } catch (OopsException | SecretsException e) {
                return new OopsResponse("mcp.auth.failed", Response.Status.UNAUTHORIZED).build();
            }
            
            String principal = jwt.getSubject();
            
            // 3. Check ACL
            try {
                checkRealmAccess(principal, realm.getSelf(), jwt);
            } catch (OopsException e) {
                return new OopsResponse("mcp.acl.denied", Response.Status.FORBIDDEN).build();
            }
            
            // 4. Find resource by URI
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            I_MCPResource resource = builder.getResources().stream()
                .filter(r -> r.matchesUri(uri))
                .findFirst()
                .orElse(null);
            
            if (resource == null) {
                log.warn("mcp.resource.notfound: {} realm={}", uri, realmStr);
                return new OopsResponse("mcp.resource.notfound", Response.Status.NOT_FOUND).build();
            }
            
            // 5. Create context and read resource
            MCPCallContext ctx = new MCPCallContext("__resource__", Map.of("uri", uri));
            ctx.set(MCPCallContext.KEY_PRINCIPAL, principal);
            ctx.set(MCPCallContext.KEY_REALM, realmStr);
            
            I_MCPResult result;
            try {
                result = resource.read(ctx, uri);
            } catch (MCPException e) {
                log.warn("mcp.resource.read.failed: {} -> {}", uri, e.getMessage());
                return new OopsResponse("mcp.resource.error: " + e.getMessage(), 
                                      Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            
            // 6. Return resource in appropriate format
            String mimeType = resource.getMimeType();
            Response.ResponseBuilder responseBuilder = Response.ok(result.getContent())
                .type(mimeType)
                .header("X-Resource-URI", uri)
                .header("X-Realm", realmStr)
                .header("X-Principal", principal);
            
            log.info("mcp.resource.read: {} realm={} principal={} mimeType={}", 
                uri, realmStr, principal, mimeType);
            
            return responseBuilder.build();
            
        } catch (Exception e) {
            log.error("readResource({}) failed", uri, e);
            return new OopsResponse("mcp.resource.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /mcp/prompts/{name} — Render a prompt template with arguments.
     *
     * <p>Prompts are parameterized LLM instruction templates. Arguments are validated
     * against the prompt's argument schema before rendering.
     *
     * @param promptName the prompt template name (e.g., "compose_query")
     * @param args argument key-value pairs
     * @param auth JWT bearer token
     * @param realmParam optional realm (default: "default")
     * @return text/plain rendered prompt
     */
    @POST
    @Path("prompts/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/plain")
    @Operation(
        summary = "Render a prompt template",
        description = "Render a prompt template with the provided arguments. Arguments must match the prompt's schema."
    )
    @APIResponse(responseCode = "200", description = "Rendered prompt text")
    @APIResponse(responseCode = "400", description = "Bad arguments (schema validation failed)")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "404", description = "Prompt not found")
    public Response renderPrompt(
            @PathParam("name") String promptName,
            Map<String, String> args,
            @HeaderParam("Authorization") String auth,
            @QueryParam("realm") String realmParam) {
        
        Instant startTime = Instant.now();
        
        try {
            // 1. Validate inputs
            if (Validate.isMissing(promptName)) {
                return new OopsResponse("mcp.prompt.name.missing", Response.Status.BAD_REQUEST).build();
            }
            
            if (args == null) {
                args = Collections.emptyMap();
            }
            
            if (Validate.isNonAlphanumeric(auth) || !auth.startsWith("Bearer ")) {
                return new OopsResponse("mcp.auth.missing", Response.Status.UNAUTHORIZED).build();
            }
            
            String realmStr = realmParam != null && !realmParam.isEmpty() 
                ? realmParam 
                : "default";
            
            // 2. Load realm and authenticate
            I_Realm realm = platform.getRealm(Values.iri(realmStr + ":"));
            if (realm == null) {
                return new OopsResponse("mcp.realm.notfound", Response.Status.NOT_FOUND).build();
            }
            
            DecodedJWT jwt;
            try {
                jwt = authenticate(auth, realm);
            } catch (OopsException | SecretsException e) {
                return new OopsResponse("mcp.auth.failed", Response.Status.UNAUTHORIZED).build();
            }
            
            String principal = jwt.getSubject();
            
            // 3. Check ACL
            try {
                checkRealmAccess(principal, realm.getSelf(), jwt);
            } catch (OopsException e) {
                return new OopsResponse("mcp.acl.denied", Response.Status.FORBIDDEN).build();
            }
            
            // 4. Find prompt by name
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            I_MCPPrompt prompt = builder.getPrompts().stream()
                .filter(p -> p.getName().equals(promptName))
                .findFirst()
                .orElse(null);
            
            if (prompt == null) {
                log.warn("mcp.prompt.notfound: {} realm={}", promptName, realmStr);
                return new OopsResponse("mcp.prompt.notfound", Response.Status.NOT_FOUND).build();
            }
            
            // 5. Validate arguments against prompt schema
            try {
                validatePromptArgs(args, prompt.getArguments());
            } catch (Exception e) {
                log.warn("mcp.prompt.validation.failed: {} -> {}", promptName, e.getMessage());
                return new OopsResponse("mcp.prompt.args.invalid: " + e.getMessage(), 
                                      Response.Status.BAD_REQUEST).build();
            }
            
            // 6. Create context and render prompt
            MCPCallContext ctx = new MCPCallContext("__prompt__", (Map) args);
            ctx.set(MCPCallContext.KEY_PRINCIPAL, principal);
            ctx.set(MCPCallContext.KEY_REALM, realmStr);
            
            I_MCPResult result;
            try {
                result = prompt.render(ctx, args);
            } catch (MCPException e) {
                log.warn("mcp.prompt.render.failed: {} -> {}", promptName, e.getMessage());
                return new OopsResponse("mcp.prompt.error: " + e.getMessage(), 
                                      Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            
            // 7. Return rendered prompt as plain text
            log.info("mcp.prompt.render: {} realm={} principal={}", promptName, realmStr, principal);
            
            return Response.ok(result.getContent()).type("text/plain").build();
            
        } catch (Exception e) {
            log.error("renderPrompt({}) failed", promptName, e);
            return new OopsResponse("mcp.prompt.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /mcp/status — Health check and server status.
     *
     * <p>Returns overall MCP subsystem health, available tool count, and server version.
     * Accessible without authentication to enable monitor health checks.
     *
     * @return JSON response with health status
     */
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "MCP server health check",
        description = "Returns overall MCP subsystem status. No authentication required."
    )
    @APIResponse(responseCode = "200", description = "Server status")
    public Response mcpStatus() {
        Instant startTime = Instant.now();
        
        try {
            MCPServerBuilder builder = toolRegistry.buildServerBuilder();
            
            int toolCount = builder.getTools().size();
            int resourceCount = builder.getResources().size();
            int promptCount = builder.getPrompts().size();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "operational");
            response.put("version", builder.getServerVersion() != null ? builder.getServerVersion() : "0.91.6");
            response.put("serverName", builder.getServerName() != null ? builder.getServerName() : "iq-mcp");
            response.put("toolCount", toolCount);
            response.put("resourceCount", resourceCount);
            response.put("promptCount", promptCount);
            response.put("transport", "http");
            response.put("timestamp", System.currentTimeMillis());
            response.put("durationMs", Duration.between(startTime, Instant.now()).toMillis());
            
            log.info("mcp.status: tools={} resources={} prompts={}", toolCount, resourceCount, promptCount);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("mcpStatus() failed", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error_message", "Status check failed: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse).build();
        }
    }

    /* ══════════════════════════════════════════════════════════════════════════
       Helper Methods — Input Validation & Error Mapping
       ══════════════════════════════════════════════════════════════════════════ */

    /**
     * Validate input JSON object against a JSON Schema.
     *
     * <p>Basic validation: checks that all required properties are present.
     * Future: integrate with a proper JSON Schema validator (networknt/json-schema-validator).
     *
     * @param input the input object to validate
     * @param schema the JSON Schema (as a Map) describing expected structure
     * @throws IllegalArgumentException if validation fails
     */
    private void validateInputSchema(Map<String, Object> input, Map<String, Object> schema) 
            throws IllegalArgumentException {
        if (schema == null || schema.isEmpty()) {
            return; // No schema = no validation
        }
        
        // Extract required fields from schema
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        if (required != null && !required.isEmpty()) {
            for (String field : required) {
                if (!input.containsKey(field)) {
                    throw new IllegalArgumentException("Missing required field: " + field);
                }
            }
        }
        
        // Validate property types (basic check)
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties != null) {
            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                String fieldName = prop.getKey();
                if (input.containsKey(fieldName)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldSchema = (Map<String, Object>) prop.getValue();
                    String expectedType = (String) fieldSchema.get("type");
                    Object value = input.get(fieldName);
                    
                    if (expectedType != null && value != null) {
                        if (!validateType(value, expectedType)) {
                            throw new IllegalArgumentException("Field '" + fieldName 
                                + "' has wrong type. Expected: " + expectedType + ", got: " 
                                + value.getClass().getSimpleName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate prompt arguments against prompt schema.
     *
     * @param args the provided arguments
     * @param schema the prompt's argument schema (list of Maps)
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePromptArgs(Map<String, String> args, List<Map<String, Object>> schema) 
            throws IllegalArgumentException {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        
        // For each expected argument, check that it's provided
        for (Map<String, Object> argSchema : schema) {
            String argName = (String) argSchema.get("name");
            Boolean required = (Boolean) argSchema.getOrDefault("required", true);
            
            if (required != null && required && (args == null || !args.containsKey(argName))) {
                throw new IllegalArgumentException("Missing required argument: " + argName);
            }
        }
    }

    /**
     * Validate that a value matches an expected JSON Schema type.
     *
     * @param value the value to check
     * @param expectedType the expected type ("string", "integer", "number", "boolean", "array", "object")
     * @return true if value matches the type
     */
    private boolean validateType(Object value, String expectedType) {
        if (value == null) {
            return "null".equals(expectedType);
        }
        
        return switch (expectedType) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List || value instanceof Object[];
            case "object" -> value instanceof Map;
            default -> true; // Unknown type = pass validation
        };
    }

    /**
     * Parse JSON string into an object.
     *
     * @param json JSON string
     * @return parsed object (Map, List, or scalar)
     * @throws Exception if parsing fails
     */
    @SuppressWarnings("unchecked")
    private Object parseJson(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        json = json.trim();
        if (json.startsWith("{")) {
            // Parse as JSON object
            return mapper.readValue(json, Map.class);
        } else if (json.startsWith("[")) {
            // Parse as JSON array
            return mapper.readValue(json, List.class);
        } else {
            // Return as string
            return json;
        }
    }

    /**
     * Map MCP exception code to HTTP status code.
     *
     * @param mcpCode MCP error code (HTTP-style: 400, 401, 403, 404, 429, 500, etc.)
     * @return HTTP status code
     */
    private int mapMCPCodeToHTTP(int mcpCode) {
        // MCP codes are HTTP-style, so usually 1:1 mapping
        return switch (mcpCode) {
            case 400, 401, 403, 404, 429 -> mcpCode;
            case 500, 502, 503, 504 -> mcpCode;
            default -> 500; // Default to internal server error for unknown codes
        };
    }
}
