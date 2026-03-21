package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import systems.symbol.mcp.connect.MCPChain;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthGuardMiddleware.
 * Tests cover: valid JWT, expired JWT, invalid signature, missing JWT, malformed JWT.
 */
class AuthGuardMiddlewareTest {

    private static final String TEST_SECRET = "test-secret-key-at-least-32-bytes-long-1234567890-x";
    private static final String TEST_ISSUER = "https://example.com";
    private static final String TEST_AUDIENCE = "test-api";

    @Test
    void testAuthGuardExtractsSubFromUnsignedJwt() throws MCPException {
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

    /**
     * Test: Valid HS256-signed JWT with correct subject is accepted.
     */
    @Test
    void testValidHS256TokenAccepted() throws MCPException {
        String token = JWT.create()
                .withSubject("bob")
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
                .sign(Algorithm.HMAC256(TEST_SECRET));

        var config = java.util.Map.of(
                "jwtSecret", TEST_SECRET,
                "jwtIssuer", TEST_ISSUER,
                "jwtAudience", TEST_AUDIENCE
        );
        AuthGuardMiddleware auth = new AuthGuardMiddleware(config);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + token);

        MCPChain chain = c -> MCPResult.okText("ok");
        I_MCPResult result = auth.process(ctx, chain);

        assertFalse(result.isError());
        assertEquals("bob", ctx.principal());
    }

    /**
     * Test: Expired JWT is rejected with 401 Unauthorized.
     */
    @Test
    void testExpiredTokenRejected() {
        String token = JWT.create()
                .withSubject("charlie")
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000)) // 1 second ago (expired)
                .sign(Algorithm.HMAC256(TEST_SECRET));

        var config = java.util.Map.of(
                "jwtSecret", TEST_SECRET,
                "jwtIssuer", TEST_ISSUER,
                "jwtAudience", TEST_AUDIENCE
        );
        AuthGuardMiddleware auth = new AuthGuardMiddleware(config);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + token);

        MCPChain chain = c -> {
            throw new AssertionError("chain should not proceed");
        };

        MCPException ex = assertThrows(MCPException.class, () -> auth.process(ctx, chain));
        assertTrue(ex.getMessage().contains("Invalid or missing JWT") || ex.getMessage().contains("verification failed"));
    }

    /**
     * Test: JWT with invalid signature is rejected.
     */
    @Test
    void testInvalidSignatureRejected() {
        String token = JWT.create()
                .withSubject("diane")
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // Tamper with the signature
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalid_signature";

        var config = java.util.Map.of(
                "jwtSecret", TEST_SECRET,
                "jwtIssuer", TEST_ISSUER,
                "jwtAudience", TEST_AUDIENCE
        );
        AuthGuardMiddleware auth = new AuthGuardMiddleware(config);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + tamperedToken);

        MCPChain chain = c -> {
            throw new AssertionError("chain should not proceed");
        };

        MCPException ex = assertThrows(MCPException.class, () -> auth.process(ctx, chain));
        assertTrue(ex.getMessage().contains("Invalid or missing JWT"));
    }

    /**
     * Test: Malformed JWT (not enough parts) falls back to anonymous.
     */
    @Test
    void testMalformedJwtFallsBackToAnonymous() throws MCPException {
        AuthGuardMiddleware auth = new AuthGuardMiddleware();
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer onlyonepart");

        MCPChain chain = c -> MCPResult.okText("ok");
        I_MCPResult result = auth.process(ctx, chain);

        assertFalse(result.isError());
        assertEquals("anonymous", ctx.principal());
    }

    /**
     * Test: Missing JWT sets principal to null; chain proceeds.
     */
    @Test
    void testMissingJwtAllowsAnonymousAccess() throws MCPException {
        AuthGuardMiddleware auth = new AuthGuardMiddleware();
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        // No JWT set

        MCPChain chain = c -> MCPResult.okText("ok");
        I_MCPResult result = auth.process(ctx, chain);

        assertFalse(result.isError());
        // Principal should be null or not set
        assertNull(ctx.principal());
    }

    /**
     * Test: JWT with wrong issuer is rejected.
     */
    @Test
    void testWrongIssuerRejected() {
        String token = JWT.create()
                .withSubject("eve")
                .withIssuer("https://wrong-issuer.com") // Wrong issuer
                .withAudience(TEST_AUDIENCE)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        var config = java.util.Map.of(
                "jwtSecret", TEST_SECRET,
                "jwtIssuer", TEST_ISSUER, // Expected issuer is different
                "jwtAudience", TEST_AUDIENCE
        );
        AuthGuardMiddleware auth = new AuthGuardMiddleware(config);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + token);

        MCPChain chain = c -> {
            throw new AssertionError("chain should not proceed");
        };

        MCPException ex = assertThrows(MCPException.class, () -> auth.process(ctx, chain));
        assertTrue(ex.getMessage().contains("Invalid or missing JWT"));
    }

    /**
     * Test: JWT with wrong audience is rejected.
     */
    @Test
    void testWrongAudienceRejected() {
        String token = JWT.create()
                .withSubject("frank")
                .withIssuer(TEST_ISSUER)
                .withAudience("wrong-audience") // Wrong audience
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        var config = java.util.Map.of(
                "jwtSecret", TEST_SECRET,
                "jwtIssuer", TEST_ISSUER,
                "jwtAudience", TEST_AUDIENCE // Expected audience is different
        );
        AuthGuardMiddleware auth = new AuthGuardMiddleware(config);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of());
        ctx.set(MCPCallContext.KEY_JWT, "Bearer " + token);

        MCPChain chain = c -> {
            throw new AssertionError("chain should not proceed");
        };

        MCPException ex = assertThrows(MCPException.class, () -> auth.process(ctx, chain));
        assertTrue(ex.getMessage().contains("Invalid or missing JWT"));
    }
}
