package systems.symbol.controller.platform;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Smoke tests for all critical MCP and platform API endpoints.
 *
 * <p>These tests ensure that published endpoints are always available and don't regress.
 * They validate both successful responses and graceful error handling.
 */
@QuarkusTest
@DisplayName("IQ Platform Smoke Tests")
public class PlatformSmokeTest {

    @BeforeEach
    void setup() {
        RestAssured.basePath = "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCP Controller Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCP: GET /mcp — Root endpoint should be available")
    void testMcpRoot() {
        given()
            .when()
            .get("/mcp")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", notNullValue())
            .body("endpoints", notNullValue())
            .body("endpoints.tools", equalTo("/mcp/tools"))
            .body("endpoints.health", equalTo("/mcp/health"));
    }

    @Test
    @DisplayName("MCP: GET /mcp/health — Health check should succeed")
    void testMcpHealth() {
        given()
            .when()
            .get("/mcp/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", notNullValue())
            .body("mcp", notNullValue());
    }

    @Test
    @DisplayName("MCP: GET /mcp/tools — Tool list endpoint should respond")
    void testMcpToolsList() {
        given()
            .when()
            .get("/mcp/tools")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503))) // 200 if ready, 503 if initializing
            .contentType(ContentType.JSON)
            .body("$", notNullValue()); // Should always return JSON
    }

    @Test
    @DisplayName("MCP: GET /mcp/resources — Resources endpoint should respond")
    void testMcpResourcesList() {
        given()
            .when()
            .get("/mcp/resources")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .contentType(ContentType.JSON)
            .body("$", notNullValue());
    }

    @Test
    @DisplayName("MCP: GET /mcp/prompts — Prompts endpoint should respond")
    void testMcpPromptsList() {
        given()
            .when()
            .get("/mcp/prompts")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .contentType(ContentType.JSON)
            .body("$", notNullValue());
    }

    @Test
    @DisplayName("MCP: POST /mcp/tools/{name}/execute — Execute endpoint should handle requests")
    void testMcpToolExecute() {
        String params = "{\"query\": \"test\"}";

        given()
            .contentType(ContentType.JSON)
            .body(params)
            .when()
            .post("/mcp/tools/test-tool/execute")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .contentType(ContentType.JSON)
            .body("$", notNullValue());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health & Readiness Checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Health: GET /health — Standard health endpoint")
    void testHealthEndpoint() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Health: GET /health/ready — Readiness probe")
    void testReadinessProbe() {
        given()
            .when()
            .get("/health/ready")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Health: GET /health/live — Liveness probe")
    void testLivenessProbe() {
        given()
            .when()
            .get("/health/live")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content Negotiation & Error Handling
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Content: All JSON endpoints should return proper content-type")
    void testContentTypeHeaders() {
        given()
            .when()
            .get("/mcp/health")
            .then()
            .header("Content-Type", containsString("application/json"));
    }

    @Test
    @DisplayName("Error: Invalid paths should return 404")
    void testInvalidPathReturns404() {
        given()
            .when()
            .get("/invalid/nonexistent/path")
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("Error: Invalid HTTP methods should be rejected")
    void testInvalidMethodReturns405() {
        given()
            .when()
            .put("/mcp/health")
            .then()
            .statusCode(405);
    }

    @Test
    @DisplayName("Error: Invalid JSON in POST should be handled")
    void testInvalidJsonHandling() {
        given()
            .contentType(ContentType.JSON)
            .body("{ invalid json }")
            .when()
            .post("/mcp/tools/test/execute")
            .then()
            .statusCode(anyOf(equalTo(400), equalTo(503))); // Bad request or service unavailable
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Response Structure Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Response: /mcp responds with proper structure")
    void testMcpResponseStructure() {
        given()
            .when()
            .get("/mcp")
            .then()
            .statusCode(200)
            .body("status", notNullValue())
            .body("message", notNullValue())
            .body("endpoints", notNullValue())
            .body("endpoints", hasKey("tools"))
            .body("endpoints", hasKey("health"));
    }

    @Test
    @DisplayName("Response: /mcp/health returns status and mcp flag")
    void testHealthResponseStructure() {
        given()
            .when()
            .get("/mcp/health")
            .then()
            .statusCode(200)
            .body("status", notNullValue())
            .body("mcp", notNullValue())
            .body("message", notNullValue());
    }

    @Test
    @DisplayName("Response: Tool list endpoints always return valid JSON")
    void testToolListResponseStructure() {
        given()
            .when()
            .get("/mcp/tools")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .body("$", notNullValue())
            .body("count", notNullValue());
    }
}
