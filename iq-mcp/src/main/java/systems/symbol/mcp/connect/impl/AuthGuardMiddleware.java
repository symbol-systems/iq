package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuthGuardMiddleware — JWT bearer token principal extraction (order 10).
 *
 * <p>This middleware extracts a bearer JWT from the incoming request (from
 * {@code mcp.jwt} in the context) and populates {@code mcp.principal}.
 *
 * <p>Secure by default: the no-arg constructor rejects all requests unless a valid
 * JWT verification config is provided. Use {@link DevAuthGuardMiddleware} explicitly
 * for development/testing without signature verification.
 */
public class AuthGuardMiddleware implements I_MCPPipeline {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthGuardMiddleware.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final IRI SELF = SimpleValueFactory.getInstance()
            .createIRI("urn:mcp:middleware:auth");

    private final JwtPrincipalExtractor principalExtractor;
    /**
     * When true (the default for all production constructors), requests with no JWT token
     * are rejected with 401 Unauthorized. Set to false only in dev/test via
     * {@link #withOptionalAuth()} or by using {@link DevAuthGuardMiddleware}.
     */
    private final boolean requiresAuthentication;

    /**
     * Default constructor — secure by default.
     * Rejects all JWTs since no verification config is present, and also rejects
     * requests with no JWT at all.
     * Use the config-driven constructor or {@link DevAuthGuardMiddleware} instead.
     */
    public AuthGuardMiddleware() {
        this(rejectAll(), true);
        log.warn("[AuthGuard] No JWT verification configured — all tokens will be REJECTED. "
                + "Provide jwtSecret/jwksUri/oidcDiscoveryUrl config, or use DevAuthGuardMiddleware for development.");
    }

    /**
     * Allows custom JWT principal extraction (e.g., using a real JWT library).
     * Authentication is required by default.
     */
    public AuthGuardMiddleware(JwtPrincipalExtractor principalExtractor) {
        this(principalExtractor, true);
    }

    /**
     * Full constructor.
     *
     * @param principalExtractor JWT-to-principal mapping strategy
     * @param requiresAuthentication when true, requests with no JWT token are rejected with 401
     */
    public AuthGuardMiddleware(JwtPrincipalExtractor principalExtractor, boolean requiresAuthentication) {
        this.principalExtractor = principalExtractor;
        this.requiresAuthentication = requiresAuthentication;
    }

    /** Returns a copy of this middleware that allows unauthenticated (no-token) requests. */
    public AuthGuardMiddleware withOptionalAuth() {
        return new AuthGuardMiddleware(this.principalExtractor, false);
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
        this(buildExtractorFromConfig(config), true);
    }

    private static JwtPrincipalExtractor buildExtractorFromConfig(java.util.Map<String, ?> config) {
        if (config == null || config.isEmpty()) {
            log.warn("[AuthGuard] Empty config — using reject-all extractor (secure default)");
            return rejectAll();
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
        log.warn("[AuthGuard] JWT config present but no verification method resolved — using reject-all");
        return rejectAll();
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
            } else if (requiresAuthentication) {
                throw MCPException.unauthorized("Authentication required: no JWT bearer token present");
            }
            return chain.proceed(ctx);
        } catch (MCPException e) {
            throw e;
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
     * Secure default: rejects all JWTs when no verification config is present.
     */
    private static JwtPrincipalExtractor rejectAll() {
        return jwt -> {
            throw new SecurityException("JWT verification not configured — token rejected. "
                    + "Configure jwtSecret, jwksUri, or oidcDiscoveryUrl.");
        };
    }

    /**
     * Package-private: returns insecure payload-only extractor for use by {@link DevAuthGuardMiddleware}.
     */
    static JwtPrincipalExtractor insecurePayloadExtractor() {
        return principalFromJwtPayload();
    }

    /**
     * SPI interface for mapping a JWT to a principal string.
     */
    public interface JwtPrincipalExtractor {
        String extractPrincipal(String jwt);
    }
}
