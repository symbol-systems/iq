package systems.symbol.mcp.connect.impl;

import org.junit.jupiter.api.Test;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import systems.symbol.mcp.connect.MCPChain;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthGuardMiddleware.
 */
class AuthGuardMiddlewareTest {

    @Test
    void testAuthGuardExtractsSubFromJwt() throws MCPException {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"alice\"}".getBytes());
        String token = header + "." + payload + ".";

        AuthGuardMiddleware auth = new AuthGuardMiddleware();
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + token);

        MCPChain chain = c -> MCPResult.okText("ok");
        I_MCPResult result = auth.process(ctx, chain);

        assertFalse(result.isError());
        assertEquals("alice", ctx.principal());
    }

    @Test
    void testAuthGuardFallsBackToAnonymousOnInvalidJwt() throws MCPException {
        AuthGuardMiddleware auth = new AuthGuardMiddleware();
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer not-a-jwt");

        MCPChain chain = c -> MCPResult.okText("ok");
        I_MCPResult result = auth.process(ctx, chain);

        assertFalse(result.isError());
        assertEquals("anonymous", ctx.principal());
    }

    @Test
    void testConfigValidationRequiresIssuerAndAudienceForJwt() {
        var config = java.util.Map.of(
                "jwtSecret", "secret123"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AuthGuardMiddleware(config));
        assertEquals("jwtIssuer is required when JWT validation is configured", ex.getMessage());

        var config2 = java.util.Map.of(
                "jwtSecret", "secret123",
                "jwtIssuer", "https://example.com"
        );

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> new AuthGuardMiddleware(config2));
        assertEquals("jwtAudience is required when JWT validation is configured", ex2.getMessage());
    }
}
