package systems.symbol.controller.platform;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCPController HTTP endpoints.
 *
 * <p>Tests the full stack: authentication, authorization, tool discovery,
 * tool execution, resource reading, and prompt rendering.
 *
 * <p>Requires a running Quarkus instance with a configured realm and JWT signing keys.
 */
@QuarkusTest
@DisplayName("MCPController Integration Tests")
public class MCPControllerIT {

    private static final String BASE_PATH = "mcp";
    private static final String DEFAULT_REALM = "test";
    private String validJWT;

    @BeforeEach
    void setUp() {
        RestAssured.basePath = "/" + BASE_PATH;
        // In a real test, generate or use a pre-signed JWT
        // For now, use a test fixture JWT (would need to be generated with valid signing key)
        validJWT = "Bearer " + generateTestJWT();
    }

    /* ════════════════════════════════════════════════════════════════════════
       Tool Discovery Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /mcp/tools should list available tools with schemas")
    void testListToolsSuccess() {
        Response response = given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/tools")
            .then()
            .statusCode(200)
            .body("realm", equalTo(DEFAULT_REALM))
            .body("principal", notNullValue())
            .body("toolCount", greaterThan(0))
            .body("tools", notNullValue())
            .body("tools[0].name", notNullValue())
            .body("tools[0].description", notNullValue())
            .body("tools[0].inputSchema", notNullValue())
            .extract()
            .response();

        // Verify response structure
        Map<String, Object> body = response.as(Map.class);
        assertNotNull(body.get("timestamp"));
        assertNotNull(body.get("durationMs"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) body.get("tools");
        assertTrue(tools.size() > 0, "Should have at least one tool");
    }

    @Test
    @DisplayName("GET /mcp/tools should return 401 without JWT")
    void testListToolsMissingAuth() {
        given()
            .when()
            .get("/tools")
            .then()
            .statusCode(401)
            .body("message", containsString("mcp.auth"));
    }

    @Test
    @DisplayName("GET /mcp/tools should return 403 for unauthorized principal")
    void testListToolsACLDenied() {
        String invalidJWT = "Bearer " + generateTestJWTForUnauthorizedUser();
        
        given()
            .header("Authorization", invalidJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/tools")
            .then()
            .statusCode(403)
            .body("message", containsString("mcp.acl"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Tool Schema Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /mcp/tools/{name} should return tool schema")
    void testDescribeToolSuccess() {
        String toolName = "sparql.query";  // Known tool in registry
        
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/tools/{name}", toolName)
            .then()
            .statusCode(200)
            .body("name", equalTo(toolName))
            .body("description", notNullValue())
            .body("inputSchema", notNullValue())
            .body("readOnly", equalTo(true))
            .body("rateLimit", notNullValue());
    }

    @Test
    @DisplayName("GET /mcp/tools/{name} should return 404 for unknown tool")
    void testDescribeToolNotFound() {
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/tools/{name}", "unknown.tool")
            .then()
            .statusCode(404)
            .body("message", containsString("mcp.tool.notfound"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Tool Execution Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("POST /mcp/tools/{name}/execute should execute tool with valid input")
    void testExecuteToolSuccess() {
        String toolName = "sparql.query";
        Map<String, Object> input = Map.of(
            "query", "SELECT ?s WHERE { ?s ?p ?o } LIMIT 1"
        );

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .contentType("application/json")
            .body(input)
            .when()
            .post("/tools/{name}/execute", toolName)
            .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("tool", equalTo(toolName))
            .body("realm", equalTo(DEFAULT_REALM))
            .body("principal", notNullValue())
            .body("result", notNullValue())
            .body("timestamp", notNullValue())
            .body("durationMs", notNullValue());
    }

    @Test
    @DisplayName("POST /mcp/tools/{name}/execute should reject invalid input schema")
    void testExecuteToolBadInput() {
        String toolName = "sparql.query";
        // Missing required 'query' field
        Map<String, Object> input = Map.of();

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .contentType("application/json")
            .body(input)
            .when()
            .post("/tools/{name}/execute", toolName)
            .then()
            .statusCode(400)
            .body("status", equalTo("error"))
            .body("error_code", equalTo(400))
            .body("error_message", containsString("required field"));
    }

    @Test
    @DisplayName("POST /mcp/tools/{name}/execute should return 404 for unknown tool")
    void testExecuteToolNotFound() {
        Map<String, Object> input = Map.of("data", "test");

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .contentType("application/json")
            .body(input)
            .when()
            .post("/tools/{name}/execute", "unknown.tool")
            .then()
            .statusCode(404)
            .body("error_code", equalTo(404));
    }

    @Test
    @DisplayName("POST /mcp/tools/{name}/execute should return 401 without JWT")
    void testExecuteToolMissingAuth() {
        Map<String, Object> input = Map.of("data", "test");

        given()
            .contentType("application/json")
            .body(input)
            .when()
            .post("/tools/{name}/execute", "sparql.query")
            .then()
            .statusCode(401);
    }

    /* ════════════════════════════════════════════════════════════════════════
       Resource Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /mcp/resources/{uri} should read namespaces resource")
    void testReadResourceSuccess() {
        String uri = "urn:iq:namespaces";

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/resources/{uri}", uri)
            .then()
            .statusCode(200)
            .header("X-Resource-URI", equalTo(uri))
            .header("X-Realm", equalTo(DEFAULT_REALM));
    }

    @Test
    @DisplayName("GET /mcp/resources/{uri} should return 404 for unknown resource")
    void testReadResourceNotFound() {
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .when()
            .get("/resources/{uri}", "urn:unknown:resource")
            .then()
            .statusCode(404)
            .body("message", containsString("mcp.resource.notfound"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Prompt Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("POST /mcp/prompts/{name} should render prompt with arguments")
    void testRenderPromptSuccess() {
        String promptName = "compose_query";
        Map<String, String> args = Map.of(
            "topic", "example"
        );

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .contentType("application/json")
            .body(args)
            .when()
            .post("/prompts/{name}", promptName)
            .then()
            .statusCode(200)
            .contentType("text/plain")
            .body(notNullValue());
    }

    @Test
    @DisplayName("POST /mcp/prompts/{name} should return 404 for unknown prompt")
    void testRenderPromptNotFound() {
        Map<String, String> args = Map.of();

        given()
            .header("Authorization", validJWT)
            .queryParam("realm", DEFAULT_REALM)
            .contentType("application/json")
            .body(args)
            .when()
            .post("/prompts/{name}", "unknown.prompt")
            .then()
            .statusCode(404)
            .body("message", containsString("mcp.prompt.notfound"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Health Check Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /mcp/status should return health status (no auth required)")
    void testStatusSuccess() {
        given()
            .when()
            .get("/status")
            .then()
            .statusCode(200)
            .body("status", equalTo("operational"))
            .body("toolCount", greaterThan(0))
            .body("resourceCount", greaterThanOrEqualTo(0))
            .body("promptCount", greaterThanOrEqualTo(0))
            .body("transport", equalTo("http"))
            .body("timestamp", notNullValue());
    }

    /* ════════════════════════════════════════════════════════════════════════
       Realm Validation Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /mcp/tools should return 400 for invalid realm")
    void testInvalidRealmParameter() {
        given()
            .header("Authorization", validJWT)
            .queryParam("realm", "; DROP TABLE users;")
            .when()
            .get("/tools")
            .then()
            .statusCode(400)
            .body("message", containsString("realm"));
    }

    @Test
    @DisplayName("GET /mcp/tools should use default realm when not specified")
    void testDefaultRealm() {
        given()
            .header("Authorization", validJWT)
            .when()
            .get("/tools")
            .then()
            .statusCode(200)
            .body("realm", equalTo("default"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Helper Methods
       ════════════════════════════════════════════════════════════════════════ */

    /**
     * Generate a valid test JWT for an authorized user.
     * In a real test environment, this would use the realm's signing key.
     *
     * @return JWT token string (without "Bearer " prefix)
     */
    private String generateTestJWT() {
        // This is a placeholder. In real tests, use a JWT library (Auth0-java, Jose4j, etc.)
        // to generate a valid token with the realm's signing key and proper claims.
        // Example claims:
        // {
        //   "sub": "testuser",
        //   "realm": "test",
        //   "aud": "http://localhost:8080",
        //   "exp": <future timestamp>,
        //   "iat": <current timestamp>
        // }
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJlYWxtIjoidGVzdCIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImV4cCI6OTk5OTk5OTk5OX0.xyz";
    }

    /**
     * Generate a JWT for a user not authorized for the test realm.
     *
     * @return JWT token string (without "Bearer " prefix)
     */
    private String generateTestJWTForUnauthorizedUser() {
        // Similar to above, but with a different audience or missing realm
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1bmF1dGhvcml6ZWQiLCJhdWQiOiJodHRwOi8vb3RoZXJyZWFsbTg2MDgwIn0.abc";
    }
}
