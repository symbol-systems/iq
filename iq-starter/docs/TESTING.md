# Testing Guide for IQ Starter

This document describes how to test the IQ Starter Kit's critical endpoints and ensure they don't regress.

## Smoke Tests (Recommended)

Automated tests that validate all published endpoints are available and respond correctly.

### Run all smoke tests

```bash
# Terminal 1: Start the server
./bin/start

# Terminal 2: Run smoke tests (when server is ready)
./bin/test-api
```

**What gets tested:**
- ✅ `/mcp` — Root MCP endpoint responds with available resources
- ✅ `/mcp/health` — Health check returns status and MCP flag
- ✅ `/mcp/tools` — Tool listing endpoint (200 or 503 if initializing)
- ✅ `/mcp/resources` — Resources listing endpoint
- ✅ `/mcp/prompts` — Prompts listing endpoint
- ✅ `/mcp/tools/{name}/execute` — Tool execution endpoint
- ✅ `/health` — Standard health endpoint
- ✅ `/health/ready` — Kubernetes readiness probe
- ✅ `/health/live` — Kubernetes liveness probe

### Run with custom port

```bash
./bin/test-api --port 8081
```

## Manual Testing (Quick checks)

### 1. Check MCP root endpoint — provides available resources

```bash
curl http://localhost:8080/mcp | jq .
```

Expected: `{"status": "ready", "endpoints": {"tools": "/mcp/tools", "resources": ...}}`

### 2. Check health status

```bash
curl http://localhost:8080/mcp/health | jq .
```

Expected: `{"status": "healthy", "mcp": true, "message": "..."}`

### 3. List available MCP tools

```bash
curl http://localhost:8080/mcp/tools | jq .
```

Expected: `{"tools": [], "count": 0, "status": "ready"}`

### 4. List resources

```bash
curl http://localhost:8080/mcp/resources | jq .
```

### 5. List prompts

```bash
curl http://localhost:8080/mcp/prompts | jq .
```

### 6. Standard health check

```bash
curl http://localhost:8080/health | jq .
```

### 7. Kubernetes probes

```bash
# Readiness (should return 200 when ready to receive traffic)
curl http://localhost:8080/health/ready | jq .

# Liveness (should always return 200 if process is alive)
curl http://localhost:8080/health/live | jq .
```

## Testing in Docker

```bash
# Build and start container
docker-compose up -d

# Run smoke tests against container
./bin/test-api

# View logs
docker-compose logs -f iq-apis

# Stop
docker-compose down
```

## Continuous Integration

The smoke tests can be integrated into CI/CD pipelines:

```bash
# In GitHub Actions, GitLab CI, or Azure Pipelines
./bin/start &
SERVER_PID=$!
sleep 30  # Wait for server startup
./bin/test-api
TEST_RESULT=$?
kill $SERVER_PID
exit $TEST_RESULT
```

## Known Endpoint Behavior

### Graceful Degradation

Endpoints should handle various initialization states:

| State | HTTP Status | Response |
|-------|------------|----------|
| Server initializing | 503 Service Unavailable | `{"error": "MCP tools not yet initialized"}` |
| Repository loading | 503 Service Unavailable | `{"error": "MCP resources not yet initialized"}` |
| Fully ready | 200 OK | `{"status": "healthy", "mcp": true}` |
| Internal error | 500 Internal Server Error | `{"error": "..."}` |

### Expected Startup Sequence

1. Server starts → Returns service unavailable (503)
2. Platform initializes repository
3. MCP tool registry loads
4. All endpoints return 200 with valid data

Total time: 30-60 seconds on first run, 5-10 seconds thereafter.

## Regression Prevention

### What gets tested

The `PlatformSmokeTest` class validates:
- **Endpoint availability** — All published URLs respond
- **Status codes** — Correct HTTP status codes (200, 503, 404, etc.)
- **Content types** — JSON responses with proper `Content-Type: application/json`
- **Response structure** — JSON has expected fields
- **Error handling** — Invalid input is handled gracefully
- **Content negotiation** — Proper headers returned

### Why it matters

Once you publish an endpoint, users depend on it:
- ❌ **Bad:** Endpoint disappears in a code refactor
- ✅ **Good:** Smoke tests catch breaking changes immediately

### Adding new endpoints

When you add a new endpoint:
1. Add a `@Test` method to `PlatformSmokeTest`
2. Describe what it tests in `@DisplayName`
3. Validate response structure and status codes
4. Run `./bin/test-api` to verify

Example:
```java
@Test
@DisplayName("API: GET /api/custom — Custom endpoint should respond")
void testCustomEndpoint() {
    given()
        .when()
        .get("/api/custom")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("field", notNullValue());
}
```

## Debugging Failed Tests

### If tests fail...

1. **Check server is running**
   ```bash
   curl http://localhost:8080/mcp/health
   ```

2. **Check logs** (from the server that was started with `./bin/start`)
   - Look for error messages
   - Note the startup sequence

3. **Run a single test**
   ```bash
   ./mvnw test -pl iq-apis -Dtest=PlatformSmokeTest#testMcpRoot -DskipITs
   ```

4. **Manual test the endpoint**
   ```bash
   curl -v http://localhost:8080/mcp
   ```

5. **Check port conflicts**
   ```bash
   lsof -i :8080
   ```

## Test Architecture

Tests use:
- **Framework:** JUnit 5 (Jupiter)
- **REST Testing:** RestAssured
- **Assertion Library:** Hamcrest matchers
- **Container:** Quarkus test framework (auto-starts server for tests)

File location: `iq-apis/src/test/java/systems/symbol/controller/platform/PlatformSmokeTest.java`

## Performance Expectations

| Test | Time | Notes |
|------|------|-------|
| Single endpoint call | <100ms | Normal response time |
| Full smoke suite | <30 seconds | Includes server initialization |
| With Docker build | 2-5 minutes | Includes image build time |

## Next Steps

1. ✅ Run `./bin/start` to verify server starts
2. ✅ Run `./bin/test-api` to verify all endpoints work
3. ✅ If deploying to cloud, run `docker-compose up` and test there too
4. ✅ For CI/CD, integrate the test script into your pipeline

**Questions?** See [FAQ.md](../docs/FAQ.md)
