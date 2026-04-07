package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuthGuardMiddleware — JWT bearer token principal extraction (order 10).
 *
 * <p>This middleware extracts a bearer JWT from the incoming request (from
 * {@code mcp.jwt} in the context) and populates {@code mcp.principal}.
 *
 * <p>⚠️ WARNING: By default this implementation does **not** verify signatures.
 * It is intended as a development stub only.
 */
public class AuthGuardMiddleware implements I_MCPPipeline {

private static final String BEARER_PREFIX = "Bearer ";
private static final IRI SELF = SimpleValueFactory.getInstance()
.createIRI("urn:mcp:middleware:auth");

private final JwtPrincipalExtractor principalExtractor;

/**
 * Default constructor loads JWT config from environment variables or system properties.
 * Checks for: MCP_JWT_SECRET, MCP_JWT_ISSUER, MCP_JWT_AUDIENCE, MCP_JWKS_URI, MCP_OIDC_DISCOVERY_URL.
 * If no config is provided and a production profile is detected, throws an exception.
 * Otherwise falls back to a stub extractor (for development).
 */
public AuthGuardMiddleware() {
this(buildExtractorFromEnvironment());
}

/**
 * Allows custom JWT principal extraction (e.g., using a real JWT library).
 */
public AuthGuardMiddleware(JwtPrincipalExtractor principalExtractor) {
this.principalExtractor = principalExtractor;
}

/**
 * Config-driven constructor. Supports these config keys:
 * <ul>
 *   <li>jwtSecret (HMAC shared secret)</li>
 *   <li>jwtIssuer</li>
 *   <li>jwtAudience</li>
 *   <li>jwksUri (JWKS endpoint for RSA/ECDSA keys)</li>
 *   <li>oidcDiscoveryUrl (OIDC discovery endpoint, e.g. https://example.com/.well-known/openid-configuration)</li>
 *   <li>jwksCacheTtlMs (time in ms to cache JWKS keys)</li>
 * </ul>
 */
public AuthGuardMiddleware(java.util.Map<String, ?> config) {
this(buildExtractorFromConfig(config));
}

private static JwtPrincipalExtractor buildExtractorFromConfig(java.util.Map<String, ?> config) {
if (config == null || config.isEmpty()) {
return principalFromJwtPayload();
}

String oidcDiscoveryUrl = asString(config.get("oidcDiscoveryUrl"));
String jwksUri = asString(config.get("jwksUri"));
String secret = asString(config.get("jwtSecret"));
String issuer = asString(config.get("jwtIssuer"));
String audience = asString(config.get("jwtAudience"));
Long cacheTtlMs = asLong(config.get("jwksCacheTtlMs"));

boolean needsJwtValidation = ((oidcDiscoveryUrl != null && !oidcDiscoveryUrl.isBlank())
|| (jwksUri != null && !jwksUri.isBlank())
|| (secret != null && !secret.isBlank()));

if (needsJwtValidation) {
if (issuer == null || issuer.isBlank()) {
throw new IllegalArgumentException("jwtIssuer is required when JWT validation is configured");
}
if (audience == null || audience.isBlank()) {
throw new IllegalArgumentException("jwtAudience is required when JWT validation is configured");
}
}

if (oidcDiscoveryUrl != null && !oidcDiscoveryUrl.isBlank()) {
return JwtPrincipalExtractors.fromOidcDiscoveryUrl(oidcDiscoveryUrl, issuer, audience, cacheTtlMs);
}
if (jwksUri != null && !jwksUri.isBlank()) {
return JwtPrincipalExtractors.fromJwksUrl(jwksUri, issuer, audience, cacheTtlMs);
}
if (secret != null && !secret.isBlank()) {
return JwtPrincipalExtractors.fromHmacSecret(secret, issuer, audience);
}
return principalFromJwtPayload();
}

private static JwtPrincipalExtractor buildExtractorFromEnvironment() {
Map<String, Object> config = new java.util.HashMap<>();

String oidcDiscoveryUrl = System.getenv("MCP_OIDC_DISCOVERY_URL");
if (oidcDiscoveryUrl != null && !oidcDiscoveryUrl.isBlank()) {
config.put("oidcDiscoveryUrl", oidcDiscoveryUrl);
}

String jwksUri = System.getenv("MCP_JWKS_URI");
if (jwksUri != null && !jwksUri.isBlank()) {
config.put("jwksUri", jwksUri);
}

String jwtSecret = System.getenv("MCP_JWT_SECRET");
if (jwtSecret != null && !jwtSecret.isBlank()) {
config.put("jwtSecret", jwtSecret);
}

String jwtIssuer = System.getenv("MCP_JWT_ISSUER");
if (jwtIssuer != null && !jwtIssuer.isBlank()) {
config.put("jwtIssuer", jwtIssuer);
}

String jwtAudience = System.getenv("MCP_JWT_AUDIENCE");
if (jwtAudience != null && !jwtAudience.isBlank()) {
config.put("jwtAudience", jwtAudience);
}

String cacheTtl = System.getenv("MCP_JWKS_CACHE_TTL_MS");
if (cacheTtl != null && !cacheTtl.isBlank()) {
config.put("jwksCacheTtlMs", cacheTtl);
}

if (!config.isEmpty()) {
return buildExtractorFromConfig(config);
}

// Check if we're in a production-like profile
String quarkusProfile = System.getenv("QUARKUS_PROFILE");
if (quarkusProfile == null) {
quarkusProfile = System.getProperty("quarkus.profile", "");
}

boolean isProduction = quarkusProfile != null && !quarkusProfile.isBlank() 
&& !quarkusProfile.equals("dev") && !quarkusProfile.equals("test");

if (isProduction) {
throw new IllegalStateException(
"JWT verification is not configured but running in production mode (" + quarkusProfile + "). "
+ "Set one of: MCP_OIDC_DISCOVERY_URL, MCP_JWKS_URI, or MCP_JWT_SECRET "
+ "(with MCP_JWT_ISSUER and MCP_JWT_AUDIENCE)."
);
}

// Development/test: warn but use stub
System.err.println("WARNING: AuthGuardMiddleware is using a stub JWT extractor (no signature verification). "
+ "This is only safe for development. To enable JWT verification, set MCP_JWT_SECRET, MCP_JWKS_URI, or MCP_OIDC_DISCOVERY_URL.");

return principalFromJwtPayload();
}

private static Long asLong(Object value) {
if (value instanceof Number n) return n.longValue();
if (value instanceof String s) {
try {
return Long.parseLong(s);
} catch (NumberFormatException e) {
return null;
}
}
return null;
}

private static String asString(Object value) {
return (value instanceof String) ? (String) value : null;
}

@Override
public IRI getSelf() {
return SELF;
}

@Override
public int getOrder() {
return 10;
}

@Override
public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
try {
String token = extractBearerToken(ctx);
if (token != null) {
String principal = principalExtractor.extractPrincipal(token);
ctx.set(MCPCallContext.KEY_PRINCIPAL, principal);
}
return chain.proceed(ctx);
} catch (Exception e) {
throw MCPException.unauthorized("Invalid or missing JWT: " + e.getMessage());
}
}

private String extractBearerToken(MCPCallContext ctx) {
// In production, extract from HTTP Authorization header via server layer
String token = ctx.get(MCPCallContext.KEY_JWT);
if (token != null && token.startsWith(BEARER_PREFIX)) {
return token.substring(BEARER_PREFIX.length());
}
return null;
}

private static JwtPrincipalExtractor principalFromJwtPayload() {
return jwt -> {
try {
String[] parts = jwt.split("\\.");
if (parts.length < 2) return "anonymous";
byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
String payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
Matcher m = Pattern.compile("\\\"sub\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
.matcher(payload);
if (m.find()) {
return m.group(1);
}
} catch (Exception ignored) {
// ignore parsing errors; fall through
}
return "anonymous";
};
}

/**
 * SPI interface for mapping a JWT to a principal string.
 */
public interface JwtPrincipalExtractor {
String extractPrincipal(String jwt);
}
}
