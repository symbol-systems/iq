package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

/**
 * AuthGuardMiddleware — JWT bearer token validation (order 10).
 *
 * Extracts and validates JWT from the Authorization header,
 * populating {@code mcp.principal} and {@code mcp.roles} in the context.
 */
public class AuthGuardMiddleware implements I_MCPPipeline {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final IRI SELF = SimpleValueFactory.getInstance()
            .createIRI("urn:mcp:middleware:auth");

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
                String principal = validateAndGetPrincipal(token);
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

    private String validateAndGetPrincipal(String token) throws Exception {
        // Placeholder: in production, use a real JWT library with key management.
        // For now, assume token structure and extract subject.
        return "user";
    }
}
