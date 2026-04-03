package systems.symbol.controller.platform;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)));
}

@Test
@DisplayName("MCP: GET /mcp/health — Health check should succeed")
void testMcpHealth() {
given()
.when()
.get("/mcp/health")
.then()
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)));
}

@Test
@DisplayName("MCP: GET /mcp/tools — Tool list endpoint should respond")
void testMcpToolsList() {
given()
.when()
.get("/mcp/tools")
.then()
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401))) // 200 if ready, 503 if initializing, 401 if auth required
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
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)))
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
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)))
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
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)))
.contentType(ContentType.JSON)
.body("$", notNullValue());
}

@Test
@DisplayName("MCP: POST /mcp — Handle MCP client messages")
void testMcpMessageHandling() {
String message = "{\"method\": \"initialize\", \"params\": {\"protocol\": \"model context protocol\", \"version\": \"1.0\"}}";

given()
.contentType(ContentType.JSON)
.body(message)
.when()
.post("/mcp")
.then()
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)));
}

@Test
@DisplayName("MCP: GET /mcp with Accept: text/event-stream — SSE connection should open")
void testMcpSseConnection() {
given()
.header("Accept", "text/event-stream")
.when()
.get("/mcp")
.then()
.statusCode(anyOf(equalTo(200), equalTo(401)));
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
@DisplayName("OAuth: POST /oauth/token returns access token")
void testOAuthTokenEndpoint() {
given()
.contentType(ContentType.URLENC)
.formParam("grant_type", "client_credentials")
.formParam("client_id", "cli")
.formParam("client_secret", "secret")
.when()
.post("/oauth/token")
.then()
.statusCode(200)
.contentType(ContentType.JSON)
.body("access_token", notNullValue())
.body("token_type", equalTo("Bearer"));
}

@Test
@DisplayName("Cluster: register and list nodes")
void testClusterNodeRegistration() {
given()
.contentType(ContentType.JSON)
.body(Map.of("url", "http://localhost:1234"))
.when()
.post("/oauth/auth/cluster/node")
.then()
.statusCode(200)
.body("registered", equalTo("http://localhost:1234"));

given()
.when()
.get("/oauth/auth/cluster/nodes")
.then()
.statusCode(200)
.body("nodes", hasItem("http://localhost:1234"));
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
.statusCode(anyOf(equalTo(404), equalTo(401)));
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
.statusCode(anyOf(equalTo(400), equalTo(503), equalTo(401))); // Bad request, service unavailable, or unauthorized
}

@Test
@DisplayName("OAuth: POST /oauth/token should issue a bearer token")
void testOauthToken() {
given()
.contentType(ContentType.URLENC)
.formParam("grant_type", "client_credentials")
.formParam("client_id", "test-client")
.formParam("client_secret", "secret")
.when()
.post("/oauth/token")
.then()
.statusCode(200)
.contentType(ContentType.JSON)
.body("access_token", notNullValue())
.body("token_type", equalTo("Bearer"));
}

@Test
@DisplayName("Cluster: POST/GET/DELETE /oauth/auth/cluster/node")
void testClusterNodeLifecycle() {
String url = "http://localhost:8085";

given()
.contentType(ContentType.JSON)
.body(Map.of("url", url))
.when()
.post("/oauth/auth/cluster/node")
.then()
.statusCode(200)
.body("registered", equalTo(url));

given()
.when()
.get("/oauth/auth/cluster/nodes")
.then()
.statusCode(200)
.body("nodes", hasItem(url));

given()
.queryParam("url", url)
.when()
.delete("/oauth/auth/cluster/node")
.then()
.statusCode(200)
.body("removed", equalTo(true));
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
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)))
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
