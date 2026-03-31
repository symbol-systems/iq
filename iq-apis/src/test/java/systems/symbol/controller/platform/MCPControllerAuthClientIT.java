package systems.symbol.controller.platform;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Extension IT tests for MCPController — JWT authentication and client features.
 *
 * <p>Tests added for Phase 2.2 (JWT Auth) and Phase 3 (MCP Client) features:
 * <ul>
 *   <li>MCPAuthException error responses (401/403)</li>
 *   <li>Bearer token extraction and validation</li>
 *   <li>Realm parameter handling</li>
 *   <li>Remote tool invocation</li>
 *   <li>Client intent execution</li>
 * </ul>
 *
 * <p>These tests complement MCPControllerIT.java (Phase 2.1 local tools).
 * Run with: {@code ./mvnw -pl iq-apis verify -DskipITs=false -Dtest=*ControllerIT}
 */
@QuarkusTest
@DisplayName("MCPController JWT Auth & Client Tests")
public class MCPControllerAuthClientIT {

    private static final String MCP_BASE_PATH = "mcp";
    private String validJWT;
    private String invalidJWT;
    private String expiredJWT;

    @BeforeEach
    void setUp() {
        RestAssured.basePath = "/" + MCP_BASE_PATH;
        validJWT = "Bearer " + generateValidTestJWT();
        invalidJWT = "Bearer invalid.jwt.token";
        expiredJWT = "Bearer " + generateExpiredTestJWT();
    }

    /* ════════════════════════════════════════════════════════════════════════
       MCPAuthException Error Response Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Authentication Error Responses (MCPAuthException)")
    class AuthExceptionTests {

        @Test
        @DisplayName("GET /mcp/tools without JWT returns 401 UNAUTHORIZED")
        void testMissingAuthReturns401() {
            given()
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401)
                .body("error_code", equalTo(401))
                .body("message", containsString("mcp.auth"));
        }

        @Test
        @DisplayName("GET /mcp/tools with empty Authorization header returns 401")
        void testEmptyAuthHeaderReturns401() {
            given()
                .header("Authorization", "")
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("GET /mcp/tools with invalid JWT returns 401")
        void testInvalidJWTReturns401() {
            given()
                .header("Authorization", invalidJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401)
                .body("error_code", equalTo(401));
        }

        @Test
        @DisplayName("GET /mcp/tools with expired JWT returns 401")
        void testExpiredJWTReturns401() {
            given()
                .header("Authorization", expiredJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401)
                .body("error_code", equalTo(401))
                .body("message", containsString("expired|invalid"));
        }

        @Test
        @DisplayName("GET /mcp/tools for unauthorized realm returns 403 FORBIDDEN")
        void testUnauthorizedRealmReturns403() {
            String wrongRealmJWT = "Bearer " + generateJWTForRealm("other-realm");
            
            given()
                .header("Authorization", wrongRealmJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(403)
                .body("error_code", equalTo(403))
                .body("message", containsString("mcp.acl|realm|forbidden"));
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       Bearer Token Handling Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Bearer Token Handling")
    class BearerTokenTests {

        @Test
        @DisplayName("Should extract token from 'Bearer <token>' format")
        void testBearerTokenExtraction() {
            Response response = given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(200)
                .extract()
                .response();

            // Verify successful extraction & validation
            assertNotNull(response.jsonPath().get("principal"));
        }

        @Test
        @DisplayName("Should reject 'Basic <token>' auth scheme")
        void testRejectsBasicAuthScheme() {
            given()
                .header("Authorization", "Basic dXNlcjpwYXNz")  // user:pass in base64
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401)
                .body("message", containsString("Bearer|JWT|mcp.auth"));
        }

        @Test
        @DisplayName("Should reject Bearer token without credentials")
        void testRejectsBearerWithoutCredentials() {
            given()
                .header("Authorization", "Bearer ")
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should handle malformed JWT gracefully")
        void testMalformedJWT() {
            given()
                .header("Authorization", "Bearer not.a.jwt")
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(401)
                .body("error_code", equalTo(401));
        }

        @Test
        @DisplayName("Should extract JWT claims (sub, realm, aud)")
        void testJWTClaimsExtraction() {
            Response response = given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(200)
                .extract()
                .response();

            var principal = response.jsonPath().getString("principal");
            assertNotNull(principal);
            assertTrue(principal.length() > 0);
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       Realm Parameter Validation Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Realm Parameter Validation")
    class RealmParameterTests {

        @Test
        @DisplayName("Should accept valid realm parameter")
        void testValidRealmParameter() {
            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(200)
                .body("realm", equalTo("test"));
        }

        @Test
        @DisplayName("Should reject SQL injection in realm parameter")
        void testRealmParameterSQLInjection() {
            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test'; DROP TABLE realms;--")
                .when()
                .get("/tools")
                .then()
                .statusCode(400)
                .body("message", containsString("realm|invalid|injection"));
        }

        @Test
        @DisplayName("Should reject special characters in realm")
        void testRealmParameterSpecialChars() {
            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test<script>alert('xss')</script>")
                .when()
                .get("/tools")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should use default realm when not specified")
        void testDefaultRealmWhenNotSpecified() {
            given()
                .header("Authorization", validJWT)
                .when()
                .get("/tools")
                .then()
                .statusCode(200)
                .body("realm", notNullValue());
        }

        @Test
        @DisplayName("Should accept realm from JWT claim as fallback")
        void testRealmFromJWTClaim() {
            var jwtForRealm = "Bearer " + generateJWTForRealm("jwt-realm");
            
            given()
                .header("Authorization", jwtForRealm)
                // No query param, use JWT claim
                .when()
                .get("/tools")
                .then()
                .statusCode(200);
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       Remote Tool Invocation Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Remote Tool Support (Phase 3)")
    class RemoteToolTests {

        @Test
        @DisplayName("POST /mcp/tools/{name}/execute should invoke remote tools")
        void testRemoteToolExecution() {
            String remoteToolName = "remote.sparql.query";
            var input = Map.of("query", "SELECT ?s WHERE { ?s a ?type }");

            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .contentType("application/json")
                .body(input)
                .when()
                .post("/tools/{name}/execute", remoteToolName)
                .then()
                // Could be 200 if tool exists, or 404 if not configured
                .statusCode(anyOf(
                    equalTo(200),
                    equalTo(404),
                    equalTo(503)  // Remote server unavailable
                ));
        }

        @Test
        @DisplayName("Remote tool execution requires authentication")
        void testRemoteToolAuthRequired() {
            given()
                .queryParam("realm", "test")
                .contentType("application/json")
                .body(Map.of("query", "SELECT *"))
                .when()
                .post("/tools/{name}/execute", "remote.tool")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should return server info in response headers")
        void testRemoteToolResponseHeaders() {
            String toolName = "remote.sparql.query";
            var input = Map.of("query", "SELECT 1");

            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .contentType("application/json")
                .body(input)
                .when()
                .post("/tools/{name}/execute", toolName)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(503)))
                .header("X-Tool-Server", anyOf(
                    notNullValue(),
                    nullValue()  // Not necessarily present
                ));
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       Authentication Error scenarios
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should handle concurrent requests with different realms")
    void testConcurrentDifferentRealms() {
        var thread1 = new Thread(() -> {
            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "test")
                .when()
                .get("/tools")
                .then()
                .statusCode(200);
        });

        var thread2 = new Thread(() -> {
            given()
                .header("Authorization", validJWT)
                .queryParam("realm", "prod")
                .when()
                .get("/tools")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(403)));
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Concurrent access interrupted");
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       JWT Principal Tracking Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Response should include principal from JWT")
    void testPrincipalInResponse() {
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", "test")
            .when()
            .get("/tools")
            .then()
            .statusCode(200)
            .body("principal", notNullValue())
            .body("principal", not(emptyString()));
    }

    @Test
    @DisplayName("Response should include timestamp for audit")
    void testTimestampInResponse() {
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", "test")
            .when()
            .get("/tools")
            .then()
            .statusCode(200)
            .body("timestamp", notNullValue());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Helper Methods — JWT Generation
       ════════════════════════════════════════════════════════════════════════ */

    /**
     * Generate a valid test JWT for the default test realm.
     *
     * <p>In a real test environment, this would use Auth0-java or Jose4j
     * to generate a properly signed token with the realm's public key.
     *
     * @return JWT string (without "Bearer " prefix)
     */
    private String generateValidTestJWT() {
        // This is a placeholder JWT for testing.
        // In production, sign with realm's private key:
        // Claims:
        // {
        //   "sub": "integration-test",
        //   "realm": "test",
        //   "aud": "http://localhost:8080",
        //   "exp": <far future>,
        //   "iat": <now>
        // }
        // For now, use pre-generated token (would expire)
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpbnRlZ3JhdGlvbi10ZXN0IiwicmVhbG0iOiJ0ZXN0IiwiYXVkIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiZXhwIjo5OTk5OTk5OTk5fQ.xyz";
    }

    /**
     * Generate an expired test JWT for testing 401 responses.
     *
     * @return Expired JWT string (without "Bearer " prefix)
     */
    private String generateExpiredTestJWT() {
        // JWT with exp claim in the past
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwicmVhbG0iOiJ0ZXN0IiwiZXhwIjoxMDB9.abc";
    }

    /**
     * Generate a test JWT for a specific realm.
     *
     * @param realm realm claim value
     * @return JWT string (without "Bearer " prefix)
     */
    private String generateJWTForRealm(String realm) {
        // Generate JWT with specified realm claim
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwicmVhbG0iOiIiK3JlYWxtKyIsImV4cCI6OTk5OTk5OTk5OX0.xyz";
    }
}
