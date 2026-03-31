package systems.symbol.mcp;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCPAuthException — authentication-specific MCP exceptions.
 *
 * <p>Tests exception creation, HTTP status mapping, factory methods, and
 * integration with general MCPException hierarchy.
 */
@DisplayName("MCPAuthException Tests")
class MCPAuthExceptionTest {

    /* ════════════════════════════════════════════════════════════════════════
       Construction Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should create exception with message and status")
    void testAuthExceptionBasicConstruction() {
        MCPAuthException ex = new MCPAuthException(
            "Invalid JWT",
            Response.Status.UNAUTHORIZED
        );

        assertEquals(401, ex.getCode());
        assertEquals("Invalid JWT", ex.getMessage());
        assertEquals(Response.Status.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create exception with message, status, and cause")
    void testAuthExceptionWithCause() {
        Exception rootCause = new IllegalArgumentException("Token malformed");
        MCPAuthException ex = new MCPAuthException(
            "Failed to parse JWT",
            Response.Status.UNAUTHORIZED,
            rootCause
        );

        assertEquals(401, ex.getCode());
        assertEquals("Failed to parse JWT", ex.getMessage());
        assertEquals(Response.Status.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals(rootCause, ex.getCause());
    }

    @Test
    @DisplayName("Should preserve HTTP status (401 for UNAUTHORIZED)")
    void testUnauthorizedStatusPreservation() {
        MCPAuthException ex = new MCPAuthException(
            "Session expired",
            Response.Status.UNAUTHORIZED
        );

        assertEquals(Response.Status.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals(401, ex.getHttpStatus().getStatusCode());
    }

    @Test
    @DisplayName("Should preserve HTTP status (403 for FORBIDDEN)")
    void testForbiddenStatusPreservation() {
        MCPAuthException ex = new MCPAuthException(
            "Insufficient permissions",
            Response.Status.FORBIDDEN
        );

        assertEquals(Response.Status.FORBIDDEN, ex.getHttpStatus());
        assertEquals(403, ex.getHttpStatus().getStatusCode());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Factory Method Tests — unauthorized()
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Factory: unauthorized() should create 401 exception")
    void testFactoryUnauthorized() {
        MCPAuthException ex = MCPAuthException.unauthorized("Missing Bearer token");

        assertEquals(401, ex.getCode());
        assertEquals("Missing Bearer token", ex.getMessage());
        assertEquals(Response.Status.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Factory: unauthorized() with cause should preserve root exception")
    void testFactoryUnauthorizedWithCause() {
        RuntimeException cause = new RuntimeException("JWT library error");
        MCPAuthException ex = MCPAuthException.unauthorized("Token validation failed", cause);

        assertEquals(401, ex.getCode());
        assertEquals(cause, ex.getCause());
        assertSame(cause, ex.getCause());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Factory Method Tests — forbidden()
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Factory: forbidden() should create 403 exception")
    void testFactoryForbidden() {
        MCPAuthException ex = MCPAuthException.forbidden("User not in required role");

        assertEquals(403, ex.getCode());
        assertEquals("User not in required role", ex.getMessage());
        assertEquals(Response.Status.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Factory: forbidden() with cause should preserve root exception")
    void testFactoryForbiddenWithCause() {
        RuntimeException cause = new RuntimeException("ACL lookup failed");
        MCPAuthException ex = MCPAuthException.forbidden("Realm access denied", cause);

        assertEquals(403, ex.getCode());
        assertEquals(cause, ex.getCause());
    }

    /* ════════════════════════════════════════════════════════════════════════
       HTTP Status Code Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should map status code 401 for UNAUTHORIZED")
    void testStatusCode401() {
        MCPAuthException ex = MCPAuthException.unauthorized("Token expired");
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("Should map status code 403 for FORBIDDEN")
    void testStatusCode403() {
        MCPAuthException ex = MCPAuthException.forbidden("Insufficient permissions");
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Should support Response.Status API")
    void testResponseStatusAPI() {
        MCPAuthException ex = new MCPAuthException(
            "test",
            Response.Status.UNAUTHORIZED
        );
        
        Response.Status status = ex.getHttpStatus();
        assertTrue(status.equals(Response.Status.UNAUTHORIZED));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getCode());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Hierarchy & Type Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should extend MCPException")
    void testExtendsBaseException() {
        MCPAuthException ex = MCPAuthException.unauthorized("test");
        assertTrue(ex instanceof MCPException);
    }

    @Test
    @DisplayName("Should convert to I_MCPResult with error status")
    void testToResultError() {
        MCPAuthException ex = MCPAuthException.unauthorized("No credentials");
        I_MCPResult result = ex.toResult();

        assertTrue(result.isError(), "Result should be error");
        assertEquals(401, result.getErrorCode(), "Error code should be 401");
    }

    @Test
    @DisplayName("Result from exception should have meaningful error message")
    void testResultErrorMessage() {
        String errorMsg = "Bearer token is invalid";
        MCPAuthException ex = MCPAuthException.unauthorized(errorMsg);
        I_MCPResult result = ex.toResult();

        assertTrue(result.isError());
        assertNotNull(result.getErrorMessage());
        assertTrue(
            result.getErrorMessage().length() > 0,
            "Error message should be non-empty"
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       Exception Chain Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should preserve cause chain when wrapped")
    void testCauseChainPreservation() {
        Exception level1 = new RuntimeException("Network timeout");
        Exception level2 = new RuntimeException("Failed to fetch JWKS", level1);
        MCPAuthException auth = new MCPAuthException(
            "JWT validation failed",
            Response.Status.UNAUTHORIZED,
            level2
        );

        assertEquals(level2, auth.getCause());
        assertEquals(level1, level2.getCause());
    }

    @Test
    @DisplayName("Should support getCause() returning null")
    void testGetCauseNull() {
        MCPAuthException ex = MCPAuthException.forbidden("Access denied");
        assertNull(ex.getCause());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Message & String Representation Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Exception message should be descriptive")
    void testMessageDescriptive() {
        String msg = "JWT signature verification failed using key from realm";
        MCPAuthException ex = new MCPAuthException(msg, Response.Status.UNAUTHORIZED);

        assertEquals(msg, ex.getMessage());
        assertTrue(ex.getMessage().length() >= 20);
    }

    @Test
    @DisplayName("toString() should include relevant information")
    void testToStringRepresentation() {
        MCPAuthException ex = MCPAuthException.unauthorized("Token expired");
        String str = ex.toString();

        assertNotNull(str);
        assertTrue(
            str.length() > 0,
            "String representation should not be empty"
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       Common Auth Error Scenarios
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should handle missing Authorization header scenario")
    void testMissingAuthorizationHeader() {
        MCPAuthException ex = MCPAuthException.unauthorized(
            "Authorization header missing or malformed"
        );

        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("Authorization"));
    }

    @Test
    @DisplayName("Should handle invalid token format scenario")
    void testInvalidTokenFormat() {
        Exception cause = new IllegalArgumentException("Not a valid JWT");
        MCPAuthException ex = MCPAuthException.unauthorized(
            "Bearer token format invalid",
            cause
        );

        assertEquals(401, ex.getCode());
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("Should handle realm mismatch scenario")
    void testRealmMismatch() {
        MCPAuthException ex = MCPAuthException.forbidden(
            "Token is for realm 'prod' but request is for realm 'test'"
        );

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("realm"));
    }

    @Test
    @DisplayName("Should handle expired token scenario")
    void testExpiredToken() {
        MCPAuthException ex = MCPAuthException.unauthorized(
            "Token expiration time has passed: exp=1234567890"
        );

        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("expiration"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Consistency Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Different factory calls should have consistent status")
    void testFactoryConsistency() {
        MCPAuthException ex1 = MCPAuthException.unauthorized("msg1");
        MCPAuthException ex2 = MCPAuthException.unauthorized("msg2");

        assertEquals(ex1.getCode(), ex2.getCode());
        assertEquals(ex1.getHttpStatus(), ex2.getHttpStatus());
    }

    @Test
    @DisplayName("Forbidden exceptions should never have status 401")
    void testForbiddenNeverUnauthorized() {
        MCPAuthException ex = MCPAuthException.forbidden("denied");
        assertNotEquals(401, ex.getCode());
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Unauthorized exceptions should never have status 403")
    void testUnauthorizedNeverForbidden() {
        MCPAuthException ex = MCPAuthException.unauthorized("no token");
        assertNotEquals(403, ex.getCode());
        assertEquals(401, ex.getCode());
    }
}
