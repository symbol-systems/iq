package systems.symbol.controller.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.auth.oauth.*;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;

import java.util.*;

/**
 * OAuth 2.0 Authorization Server endpoints.
 * Supports:
 * - Token issuance (authorization_code, refresh_token, client_credentials)
 * - RFC 8628 Device Authorization Grant for headless clients (CLI, MCP)
 * - Token introspection (RFC 7662)
 * - Token revocation (RFC 7009)
 * - JWKS endpoint for public key distribution
 * - OIDC discovery document
 */
@Path("oauth")
public class OAuthAPI {

private static final Logger log = LoggerFactory.getLogger(OAuthAPI.class);
private static final ObjectMapper mapper = new ObjectMapper();

@Inject
OAuthAuthorizationServer authServer;

@ConfigProperty(name = "iq.oauth.issuer", defaultValue = "http://localhost:8080")
String issuer;

@Context
UriInfo info;

@Context
HttpHeaders headers;

/**
 * GET /.well-known/openid-configuration — OIDC Discovery Document
 */
@GET
@Path(".well-known/openid-configuration")
@Produces(MediaType.APPLICATION_JSON)
public Response oidcConfiguration() {
try {
Map<String, Object> config = new LinkedHashMap<>();
config.put("issuer", issuer);
config.put("authorization_endpoint", issuer + "/oauth/authorize");
config.put("token_endpoint", issuer + "/oauth/token");
config.put("introspection_endpoint", issuer + "/oauth/introspect");
config.put("revocation_endpoint", issuer + "/oauth/revoke");
config.put("jwks_uri", issuer + "/oauth/jwks");
config.put("device_authorization_endpoint", issuer + "/oauth/device/code");
config.put("scopes_supported", new String[]{
"chat.read", "chat.write", "agent.trigger",
"sparql.select", "sparql.update",
"realm.admin", "connector.execute",
"control.read", "control.write",
"policy.read", "policy.write",
"audit.read",
"fedx.join", "fedx.query"
});
config.put("response_types_supported", new String[]{"code", "token"});
config.put("grant_types_supported", new String[]{
"authorization_code",
"refresh_token",
"client_credentials",
"urn:ietf:params:oauth:grant-type:device_code"
});
config.put("token_endpoint_auth_methods_supported", new String[]{
"client_secret_basic",
"client_secret_post"
});
config.put("service_documentation", "https://docs.symbol.systems/oauth");

return Response.ok(config).build();
} catch (Exception e) {
log.error("oidc.config.error", e);
return new OopsResponse("oidc.config.error", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

/**
 * GET /oauth/jwks — JWKS Public Key Endpoint
 */
@GET
@Path("jwks")
@Produces(MediaType.APPLICATION_JSON)
public Response jwks() {
try {
Map<String, Object> jwks = new LinkedHashMap<>();
List<Map<String, Object>> keys = new ArrayList<>();

// Add public key from token factory
java.security.KeyPair keyPair = authServer.getTokenFactory().getKeyPair();
java.security.interfaces.RSAPublicKey pubKey = 
(java.security.interfaces.RSAPublicKey) keyPair.getPublic();

Map<String, Object> key = new LinkedHashMap<>();
key.put("kty", "RSA");
key.put("kid", authServer.getTokenFactory().getKid());
key.put("use", "sig");
key.put("alg", "RS256");
key.put("n", Base64.getUrlEncoder().withoutPadding()
.encodeToString(pubKey.getModulus().toByteArray()));
key.put("e", Base64.getUrlEncoder().withoutPadding()
.encodeToString(pubKey.getPublicExponent().toByteArray()));

keys.add(key);
jwks.put("keys", keys);

return Response.ok(jwks).build();
} catch (Exception e) {
log.error("jwks.error", e);
return new OopsResponse("jwks.error", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

/**
 * POST /oauth/token — Token Endpoint (RFC 6749)
 * Supports: authorization_code, refresh_token, client_credentials, RFC 8628 device_code
 */
@POST
@Path("token")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response token(
@FormParam("grant_type") String grantType,
@FormParam("client_id") String clientId,
@FormParam("client_secret") String clientSecret,
@FormParam("code") String code,
@FormParam("refresh_token") String refreshToken,
@FormParam("device_code") String deviceCode,
@FormParam("scope") String scope,
@FormParam("redirect_uri") String redirectUri,
@FormParam("code_verifier") String codeVerifier) {

try {
if (clientId == null || clientId.isBlank()) {
return errorResponse("invalid_client", "client_id required");
}

if (grantType == null || grantType.isBlank()) {
return errorResponse("invalid_request", "grant_type required");
}

// Validate client
if (!authServer.getClientRegistry().validateClientSecret(clientId, clientSecret)) {
log.warn("oauth.token.invalid_client: {}", clientId);
return errorResponse("invalid_client", "Client authentication failed");
}

// RFC 8628 Device Authorization Grant
if ("urn:ietf:params:oauth:grant-type:device_code".equals(grantType)) {
if (deviceCode == null || deviceCode.isBlank()) {
return errorResponse("invalid_request", "device_code required");
}

DeviceCodeStore.DeviceCodeRequest request = 
authServer.getDeviceCodeStore().get(deviceCode);

if (request == null) {
return errorResponse("invalid_grant", "device_code expired or invalid");
}

if (!request.isApproved()) {
return errorResponse("authorization_pending", 
"User has not yet approved the device");
}

// Exchange device code for tokens
OAuthAuthorizationServer.TokenExchangeResult result = 
authServer.completeDeviceFlow(deviceCode, clientId);

if (!result.success) {
return errorResponse(result.errorCode, result.errorDescription);
}

Map<String, Object> response = new LinkedHashMap<>();
response.put("access_token", result.accessToken);
response.put("refresh_token", result.refreshToken);
response.put("expires_in", result.expiresIn);
response.put("token_type", result.tokenType);

log.info("oauth.token.device_flow.success: {}", clientId);
return Response.ok(response).build();
}

// OAuth 2.0 Authorization Code Grant (RFC 6749)
if ("authorization_code".equals(grantType)) {
if (code == null || code.isBlank()) {
return errorResponse("invalid_request", "code required");
}
if (redirectUri == null || redirectUri.isBlank()) {
return errorResponse("invalid_request", "redirect_uri required");
}

OAuthAuthorizationServer.TokenExchangeResult result =
authServer.completeAuthorizationCodeFlow(code, clientId, redirectUri, codeVerifier);

if (!result.success) {
return errorResponse(result.errorCode, result.errorDescription);
}

Map<String, Object> response = new LinkedHashMap<>();
response.put("access_token", result.accessToken);
response.put("refresh_token", result.refreshToken);
response.put("expires_in", result.expiresIn);
response.put("token_type", result.tokenType);

log.info("oauth.token.authorization_code.success: {}", clientId);
return Response.ok(response).build();
}

// OAuth 2.0 Refresh Token Grant (RFC 6749 Section 6)
if ("refresh_token".equals(grantType)) {
if (refreshToken == null || refreshToken.isBlank()) {
return errorResponse("invalid_request", "refresh_token required");
}

String[] requestedScopes = scope != null ? scope.split(" ") : null;
OAuthAuthorizationServer.TokenExchangeResult result =
authServer.refreshAccessToken(refreshToken, clientId, requestedScopes);

if (!result.success) {
return errorResponse(result.errorCode, result.errorDescription);
}

Map<String, Object> response = new LinkedHashMap<>();
response.put("access_token", result.accessToken);
response.put("refresh_token", result.refreshToken);
response.put("expires_in", result.expiresIn);
response.put("token_type", result.tokenType);

log.info("oauth.token.refresh_token.success: {}", clientId);
return Response.ok(response).build();
}

// Client Credentials Grant
if ("client_credentials".equals(grantType)) {
String[] scopes = scope != null ? scope.split(" ") : new String[0];
String accessToken = authServer.issueAccessToken("client:" + clientId, scopes, "default", clientId);

Map<String, Object> response = new LinkedHashMap<>();
response.put("access_token", accessToken);
response.put("expires_in", 3600);
response.put("token_type", "Bearer");
response.put("scope", scope);

return Response.ok(response).build();
}

return errorResponse("unsupported_grant_type", 
"grant_type '" + grantType + "' not supported");

} catch (Exception e) {
log.error("oauth.token.error", e);
return errorResponse("server_error", e.getMessage());
}
}

/**
 * POST /oauth/introspect — Token Introspection (RFC 7662)
 */
@POST
@Path("introspect")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response introspect(
@FormParam("token") String token,
@FormParam("token_type_hint") String tokenTypeHint,
@FormParam("client_id") String clientId,
@FormParam("client_secret") String clientSecret) {

try {
if (token == null || token.isBlank()) {
return errorResponse("invalid_request", "token required");
}

// Optional client authentication
if (clientId != null && !clientId.isBlank()) {
if (!authServer.getClientRegistry().validateClientSecret(clientId, clientSecret)) {
return errorResponse("invalid_client", "Client authentication failed");
}
}

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(token);

Map<String, Object> response = new LinkedHashMap<>();
response.put("active", intro.active);

if (intro.active) {
response.put("sub", intro.sub);
response.put("iss", intro.iss);
response.put("realm", intro.realm);
response.put("scope", String.join(" ", intro.scopes));
if (intro.exp != null) {
response.put("exp", intro.exp.getEpochSecond());
}
} else if (intro.error != null) {
response.put("error", intro.error);
}

return Response.ok(response).build();
} catch (Exception e) {
log.error("oauth.introspect.error", e);
return errorResponse("server_error", e.getMessage());
}
}

/**
 * POST /oauth/revoke — Token Revocation (RFC 7009)
 */
@POST
@Path("revoke")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response revoke(
@FormParam("token") String token,
@FormParam("token_type_hint") String tokenTypeHint,
@FormParam("client_id") String clientId,
@FormParam("client_secret") String clientSecret) {

try {
if (token == null || token.isBlank()) {
return errorResponse("invalid_request", "token required");
}

// Validate client
if (clientId == null || clientId.isBlank() ||
!authServer.getClientRegistry().validateClientSecret(clientId, clientSecret)) {
log.warn("oauth.revoke.invalid_client: {}", clientId);
return errorResponse("invalid_client", "Client authentication failed");
}

// Extract and revoke JTI
try {
com.auth0.jwt.JWT jwt = new com.auth0.jwt.JWT();
String jti = jwt.decode(token).getClaim("jti").asString();
if (jti != null) {
authServer.revokeToken(jti);
log.info("oauth.revoke.success: jti={}", jti);
}
} catch (Exception e) {
// Token may be invalid/expired - that's ok for revocation
log.debug("oauth.revoke.decode_error: {}", e.getMessage());
}

// RFC 7009: Always return 200 OK for revocation
return Response.ok().build();
} catch (Exception e) {
log.error("oauth.revoke.error", e);
// Still return 200 per RFC 7009
return Response.ok().build();
}
}

/**
 * POST /oauth/device/code — RFC 8628 Device Authorization Request
 */
@POST
@Path("device/code")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response deviceCode(
@FormParam("client_id") String clientId,
@FormParam("scope") String scope) {

try {
if (clientId == null || clientId.isBlank()) {
return errorResponse("invalid_client", "client_id required");
}

if (!authServer.getClientRegistry().isGrantTypeAllowed(clientId, 
"urn:ietf:params:oauth:grant-type:device_code")) {
return errorResponse("unauthorized_client", 
"Client not authorized for device_code grant");
}

String[] scopes = scope != null ? scope.split(" ") : new String[0];
DeviceCodeStore.DeviceCodeRequest request = 
authServer.initiateDeviceFlow("default", scopes);

Map<String, Object> response = new LinkedHashMap<>();
response.put("device_code", request.deviceCode);
response.put("user_code", request.userCode);
response.put("verification_uri", request.verificationUri);
response.put("verification_uri_complete", 
request.verificationUri + "?user_code=" + request.userCode);
response.put("expires_in", request.expiresIn);
response.put("interval", request.interval);

log.info("oauth.device_code.issued: {}@{}", request.userCode, clientId);
return Response.ok(response).build();
} catch (Exception e) {
log.error("oauth.device_code.error", e);
return errorResponse("server_error", e.getMessage());
}
}

/**
 * GET /oauth/device/activate — Device Code Activation UI
 * User enters their user_code and approves the device
 */
@GET
@Path("device/activate")
@Produces(MediaType.TEXT_HTML)
public Response deviceActivateUI(
@QueryParam("user_code") String userCode) {

try {
String html = "<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"<head>\n" +
"  <meta charset=\"UTF-8\">\n" +
"  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
"  <title>IQ Device Activation</title>\n" +
"  <style>\n" +
"body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
"   margin: 0; padding: 20px; background: #f5f5f5; }\n" +
".container { max-width: 600px; margin: 0 auto; background: white; " +
"  padding: 40px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
"h1 { color: #333; margin-bottom: 30px; }\n" +
".code-input { font-size: 24px; padding: 12px; width: 100%; max-width: 300px; " +
"   letter-spacing: 4px; text-align: center; font-weight: bold; " +
"   border: 2px solid #ddd; border-radius: 4px; }\n" +
"button { background: #007bff; color: white; padding: 12px 30px; " +
" border: none; border-radius: 4px; font-size: 16px; " +
" cursor: pointer; margin-top: 20px; }\n" +
"button:hover { background: #0056b3; }\n" +
".info { color: #666; margin-top: 20px; font-size: 14px; }\n" +
".error { color: #d32f2f; margin-top: 10px; }\n" +
".success { color: #388e3c; margin-top: 10px; }\n" +
"  </style>\n" +
"</head>\n" +
"<body>\n" +
"  <div class=\"container\">\n" +
"<h1>🔐 Device Activation</h1>\n" +
"<p>Enter the code from your device to grant it access:</p>\n" +
"<form id=\"form\" onsubmit=\"approve(event)\">\n" +
"  <input type=\"text\" id=\"userCode\" class=\"code-input\" " +
" placeholder=\"XXXXXXXX\" " +
(userCode != null ? "value=\"" + userCode + "\" " : "") +
" autofocus required pattern=\"[A-Z0-9]{8,}\" " +
" minlength=\"8\" maxlength=\"8\">\n" +
"  <br>\n" +
"  <button type=\"submit\">Approve Device</button>\n" +
"</form>\n" +
"<div id=\"message\"></div>\n" +
"<div class=\"info\">\n" +
"  <p>📱 Your device is waiting for approval.</p>\n" +
"  <p>🔒 Only approve if YOU initiated this request.</p>\n" +
"</div>\n" +
"  </div>\n" +
"  <script>\n" +
"function approve(event) {\n" +
"  event.preventDefault();\n" +
"  const userCode = document.getElementById('userCode').value;\n" +
"  const messageDiv = document.getElementById('message');\n" +
"  \n" +
"  fetch('/oauth/device/approve', {\n" +
"method: 'POST',\n" +
"headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
"body: 'user_code=' + encodeURIComponent(userCode)\n" +
"  })\n" +
"  .then(r => r.json())\n" +
"  .then(data => {\n" +
"if (data.success) {\n" +
"  messageDiv.innerHTML = '<div class=\"success\">✓ Device approved! ' +\n" +
"  'You can close this window.</div>';\n" +
"} else {\n" +
"  messageDiv.innerHTML = '<div class=\"error\">✗ ' + (data.error || 'Invalid code') + '</div>';\n" +
"}\n" +
"  })\n" +
"  .catch(err => messageDiv.innerHTML = '<div class=\"error\">✗ Error: ' + err + '</div>');\n" +
"}\n" +
"  </script>\n" +
"</body>\n" +
"</html>\n";

return Response.ok(html).build();
} catch (Exception e) {
log.error("device.activate.error", e);
return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
.entity("Error loading activation page").build();
}
}

/**
 * POST /oauth/device/approve — Approve a device code
 * Called from the device activation UI
 */
@POST
@Path("device/approve")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response deviceApprove(
@FormParam("user_code") String userCode) {

try {
if (userCode == null || userCode.isBlank()) {
Map<String, Object> response = new LinkedHashMap<>();
response.put("success", false);
response.put("error", "user_code required");
return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
}

// For now, we use a simple "admin" principal
// In a real system, this would require authentication
authServer.getDeviceCodeStore().approve(userCode, "admin");

Map<String, Object> response = new LinkedHashMap<>();
response.put("success", true);
response.put("message", "Device approved");

log.info("oauth.device.approved: {}", userCode);
return Response.ok(response).build();
} catch (Exception e) {
log.error("device.approve.error", e);
Map<String, Object> response = new LinkedHashMap<>();
response.put("success", false);
response.put("error", e.getMessage());
return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
}
}

// ─────────────────────────────────────────────────────────────────────────
// Cluster Management Endpoints (under /oauth/auth/cluster)
// ─────────────────────────────────────────────────────────────────────────
// Simple in-memory cluster state for testing
private static final Map<String, Map<String, Object>> clusterNodes = new java.util.concurrent.ConcurrentHashMap<>();
private static String currentLeader = null;

/**
 * POST /oauth/auth/cluster/node — Register a cluster node
 * Request: { "url": "http://localhost:8080", "role": "WORKER" | "MASTER" }
 * Response: { "registered": "http://localhost:8080" }
 */
@POST
@Path("auth/cluster/node")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response registerClusterNode(Map<String, Object> request) {
try {
String url = (String) request.get("url");
String role = (String) request.getOrDefault("role", "WORKER");

if (url == null || url.isBlank()) {
return Response.status(400).entity("url is required").build();
}

Map<String, Object> node = new LinkedHashMap<>();
node.put("url", url);
node.put("role", role);
node.put("registered", System.currentTimeMillis());
node.put("heartbeat", System.currentTimeMillis());

clusterNodes.put(url, node);

// Auto-elect MASTER as leader
if ("MASTER".equals(role) && currentLeader == null) {
currentLeader = url;
}

log.info("cluster.node.registered: {}", url);

Map<String, Object> response = new LinkedHashMap<>();
response.put("registered", url);
return Response.ok(response).build();
} catch (Exception e) {
log.error("cluster.node.registration.error", e);
return Response.status(500).entity("Registration failed: " + e.getMessage()).build();
}
}

/**
 * GET /oauth/auth/cluster/nodes — List all registered cluster nodes
 * Response: { "nodes": ["http://localhost:8080", "http://localhost:8081"] }
 */
@GET
@Path("auth/cluster/nodes")
@Produces(MediaType.APPLICATION_JSON)
public Response listClusterNodes() {
try {
Map<String, Object> response = new LinkedHashMap<>();
response.put("nodes", clusterNodes.keySet());
return Response.ok(response).build();
} catch (Exception e) {
log.error("cluster.nodes.list.error", e);
return Response.status(500).entity("Failed to list nodes").build();
}
}

/**
 * DELETE /oauth/auth/cluster/node — Unregister a cluster node
 * Query: ?url=http://localhost:8080
 * Response: { "removed": true }
 */
@DELETE
@Path("auth/cluster/node")
@Produces(MediaType.APPLICATION_JSON)
public Response unregisterClusterNode(@QueryParam("url") String url) {
try {
if (url == null || url.isBlank()) {
return Response.status(400).entity("url is required").build();
}

boolean removed = clusterNodes.remove(url) != null;

// Clear leader if it was removed
if (url.equals(currentLeader)) {
currentLeader = null;
}

log.info("cluster.node.unregistered: {}", url);

Map<String, Object> response = new LinkedHashMap<>();
response.put("removed", removed);
return Response.ok(response).build();
} catch (Exception e) {
log.error("cluster.node.unregistration.error", e);
return Response.status(500).entity("Unregistration failed").build();
}
}

/**
 * POST /oauth/auth/cluster/node/heartbeat — Send node heartbeat
 * Request: { "url": "http://localhost:8080" }
 * Response: { "heartbeat": true, "lastHeartbeat": 1234567890 }
 */
@POST
@Path("auth/cluster/node/heartbeat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response sendClusterNodeHeartbeat(Map<String, Object> request) {
try {
String url = (String) request.get("url");

if (url == null || url.isBlank()) {
return Response.status(400).entity("url is required").build();
}

Map<String, Object> node = clusterNodes.get(url);
if (node == null) {
return Response.status(404).entity("Node not registered").build();
}

long now = System.currentTimeMillis();
node.put("heartbeat", now);

Map<String, Object> response = new LinkedHashMap<>();
response.put("heartbeat", true);
response.put("lastHeartbeat", now);

return Response.ok(response).build();
} catch (Exception e) {
log.error("cluster.heartbeat.error", e);
return Response.status(500).entity("Heartbeat failed").build();
}
}

/**
 * GET /oauth/auth/cluster/leader — Get current cluster leader
 * Response: { "leader": { "url": "http://localhost:8088", "role": "MASTER" } }
 */
@GET
@Path("auth/cluster/leader")
@Produces(MediaType.APPLICATION_JSON)
public Response getClusterLeader() {
try {
if (currentLeader == null || !clusterNodes.containsKey(currentLeader)) {
// Auto-elect first MASTER or any node
for (var entry : clusterNodes.entrySet()) {
if ("MASTER".equals(entry.getValue().get("role"))) {
currentLeader = entry.getKey();
break;
}
}

if (currentLeader == null && !clusterNodes.isEmpty()) {
currentLeader = clusterNodes.keySet().iterator().next();
}
}

if (currentLeader == null) {
return Response.status(204).build(); // No leader
}

Map<String, Object> response = new LinkedHashMap<>();
Map<String, Object> leader = new LinkedHashMap<>(clusterNodes.get(currentLeader));
response.put("leader", leader);

return Response.ok(response).build();
} catch (Exception e) {
log.error("cluster.leader.error", e);
return Response.status(500).entity("Failed to get leader").build();
}
}

/**
 * Helper to create error response following OAuth error format
 */
private Response errorResponse(String error, String errorDescription) {
Map<String, Object> response = new LinkedHashMap<>();
response.put("error", error);
if (errorDescription != null) {
response.put("error_description", errorDescription);
}
return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
}
}
