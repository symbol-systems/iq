package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

/**
 * Development-only auth middleware that extracts JWT subject without signature verification.
 *
 * <p><b>WARNING:</b> This middleware does NOT verify JWT signatures. It extracts the {@code sub}
 * claim directly from the JWT payload. Only use this in development or testing environments.</p>
 *
 * <p>For production, use {@link AuthGuardMiddleware} with proper JWT verification config
 * (jwtSecret, jwksUri, or oidcDiscoveryUrl).</p>
 */
public class DevAuthGuardMiddleware implements I_MCPPipeline {

    private static final Logger log = LoggerFactory.getLogger(DevAuthGuardMiddleware.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final IRI SELF = SimpleValueFactory.getInstance()
            .createIRI("urn:mcp:middleware:auth:dev");

    private final AuthGuardMiddleware.JwtPrincipalExtractor principalExtractor;

    public DevAuthGuardMiddleware() {
        this.principalExtractor = AuthGuardMiddleware.insecurePayloadExtractor();
        log.warn("╔══════════════════════════════════════════════════════════════╗");
        log.warn("║  DEV AUTH MODE — JWT signatures are NOT verified!           ║");
        log.warn("║  Do NOT use DevAuthGuardMiddleware in production.           ║");
        log.warn("╚══════════════════════════════════════════════════════════════╝");
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
                log.debug("[DevAuth] Extracted principal (unverified): {}", principal);
            } else {
                log.debug("[DevAuth] No bearer token — proceeding as anonymous");
            }
            return chain.proceed(ctx);
        } catch (Exception e) {
            throw MCPException.unauthorized("Invalid JWT: " + e.getMessage());
        }
    }

    private String extractBearerToken(MCPCallContext ctx) {
        String token = ctx.get(MCPCallContext.KEY_JWT);
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
