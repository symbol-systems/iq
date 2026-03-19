package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.util.Base64;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

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
 * Default constructor uses a best-effort subject extractor.
 */
public AuthGuardMiddleware() {
this(principalFromJwtPayload());
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
 * </ul>
 */
public AuthGuardMiddleware(java.util.Map<String, Object> config) {
this(buildExtractorFromConfig(config));
}

private static JwtPrincipalExtractor buildExtractorFromConfig(java.util.Map<String, Object> config) {
if (config == null || config.isEmpty()) {
return principalFromJwtPayload();
}

String jwksUri = asString(config.get("jwksUri"));
String secret = asString(config.get("jwtSecret"));
String issuer = asString(config.get("jwtIssuer"));
String audience = asString(config.get("jwtAudience"));
Long cacheTtlMs = asLong(config.get("jwksCacheTtlMs"));

if (jwksUri != null && !jwksUri.isBlank()) {
return JwtPrincipalExtractors.fromJwksUrl(jwksUri, issuer, audience, cacheTtlMs);
}
if (secret != null && !secret.isBlank()) {
return JwtPrincipalExtractors.fromHmacSecret(secret, issuer, audience);
}
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
