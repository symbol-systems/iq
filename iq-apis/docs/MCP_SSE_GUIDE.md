# MCP Server-Sent Events (SSE) Implementation Guide

## Overview

The `/mcp` endpoint now supports **Server-Sent Events (SSE)** for persistent, bidirectional streaming connections with MCP clients. This allows LLMs and tools to maintain long-lived connections and receive real-time updates from the IQ server.

## Protocol Support

The `/mcp` endpoint now supports **content negotiation** to serve both protocols:

### 1. JSON REST (Default)
```bash
curl http://localhost:8080/mcp \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "status": "ready",
  "message": "MCP Server operational",
  "version": "1.0",
  "endpoints": {
"tools": "/mcp/tools",
"resources": "/mcp/resources",
"prompts": "/mcp/prompts",
"health": "/mcp/health",
"sse": "GET (Accept: text/event-stream)"
  }
}
```

### 2. Server-Sent Events (SSE)
```bash
curl -N http://localhost:8080/mcp \
  -H "Accept: text/event-stream"
```

**Response Stream:**
```
data: {"protocol": "model context protocol", "version": "1.0", "capabilities": {"tools": true, "resources": true, "prompts": true}}
id: 1711886825000

data: {"status": "ready", "message": "MCP Server operational"}
id: 1711886825001
```

## SSE Connection Flow

### 1. Client Opens SSE Connection
```bash
GET /mcp HTTP/1.1
Accept: text/event-stream
Connection: keep-alive
```

### 2. Server Sends Initialization Handshake
- **Event:** `initialization`
- **Payload:** Protocol version and capabilities
- **ID:** Timestamp-based unique event ID

### 3. Server Sends Ready Event
- **Event:** `ready`
- **Payload:** Service status
- **ID:** Unique timestamped ID

### 4. Connection Stays Open
The connection remains open for:
- Client → Server messages via `POST /mcp`
- Server → Client updates via SSE stream

### 5. Client Sends Messages
```bash
POST /mcp HTTP/1.1
Content-Type: application/json

{
  "method": "initialize",
  "params": {
"protocol": "model context protocol",
"version": "1.0"
  }
}
```

### 6. Server Acknowledges
```json
{
  "status": "acknowledged",
  "message": {
"method": "initialize",
"params": { ... }
  }
}
```

## Testing SSE Connection

### Using curl (Recommended)
```bash
# Start SSE stream (uses -N to disable buffering)
curl -N http://localhost:8080/mcp \
  -H "Accept: text/event-stream" \
  -v

# In another terminal, send a message to the same server
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"method":"initialize","params":{"protocol":"model context protocol","version":"1.0"}}'
```

### Using netcat
```bash
# Watch SSE events in real-time
nc -l -p 9999 | curl -N http://localhost:8080/mcp \
  -H "Accept: text/event-stream"
```

### Using RestAssured (Programmatic Testing)
```java
@Test
void testMcpSseConnection() {
given()
.header("Accept", "text/event-stream")
.when()
.get("/mcp")
.then()
.statusCode(200)
.contentType(containsString("event-stream"));
}
```

## SSE Implementation Details

### Controller Changes
**File:** [iq-apis/src/main/java/systems/symbol/controller/platform/MCPController.java](../src/main/java/systems/symbol/controller/platform/MCPController.java)

**Added Imports:**
- `jakarta.ws.rs.sse.OutboundSseEvent` — Individual events
- `jakarta.ws.rs.sse.Sse` — SSE context
- `jakarta.ws.rs.sse.SseEventSink` — Event stream sink
- `java.util.concurrent.atomic.AtomicLong` — Thread-safe event ID counter

**Modified Methods:**

1. **`root()`** — Now accepts SSE injection
```java
@GET
public Response root(@Context(required = false) SseEventSink sseEventSink, 
 @Context(required = false) Sse sse)
```
- Checks if SSE requested (non-null injection)
- Routes to `handleMcpSseConnection()` for SSE
- Returns JSON status for regular HTTP

2. **`handleMcpSseConnection()`** — NEW
- Sends initialization handshake event
- Sends ready event
- Keeps connection open for bidirectional communication

3. **`handleMcpMessage()`** — NEW POST handler
```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
public Response handleMcpMessage(String messageJson)
```
- Receives MCP client messages
- Acknowledges receipt
- Returns server response

### Test Coverage
**File:** [iq-apis/src/test/java/systems/symbol/controller/platform/PlatformSmokeTest.java](../src/test/java/systems/symbol/controller/platform/PlatformSmokeTest.java)

**New Test Cases:**

1. **`testMcpMessageHandling()`**
   - Tests POST `/mcp` with JSON message
   - Expects: 200 or 503 status with JSON response

2. **`testMcpSseConnection()`**
   - Tests GET `/mcp` with `Accept: text/event-stream` header
   - Expects: 200 status with `text/event-stream` content type

## Endpoint Reference

| Endpoint | Method | Accept | Purpose | Status |
|----------|--------|--------|---------|--------|
| `/mcp` | GET | `application/json` | Root status | ✅ Ready |
| `/mcp` | GET | `text/event-stream` | SSE stream | ✅ Ready |
| `/mcp` | POST | `application/json` | Send message | ✅ Ready |
| `/mcp/tools` | GET | `application/json` | List tools | ✅ Ready |
| `/mcp/resources` | GET | `application/json` | List resources | ✅ Ready |
| `/mcp/prompts` | GET | `application/json` | List prompts | ✅ Ready |
| `/mcp/tools/{name}/execute` | POST | `application/json` | Execute tool | ✅ Ready |
| `/mcp/health` | GET | `application/json` | Health check | ✅ Ready |

## Error Handling

### MCP Not Initialized
```json
{
  "status": "partial",
  "message": "MCP initializing...",
  "endpoints": { ... }
}
```

### SSE Connection Error
Connection closes with error logged. Client should reconnect with exponential backoff.

### Message Handling Error
```json
{
  "error": "MCP not yet initialized"
}
```

## Performance Considerations

1. **SSE Overhead:** Minimal compared to polling
2. **Connection Limits:** Configure Quarkus thread pool for concurrent SSE connections
3. **Memory:** Each SSE connection maintains one thread + event buffer in memory
4. **Graceful Shutdown:** SSE connections close cleanly on server shutdown

## Configuration

### Quarkus Settings (application.properties)
```properties
# SSE connection timeout (default: 30 seconds)
quarkus.http.so-timeout=30s

# Thread pool for SSE connections
quarkus.thread-pool.core-size=10
quarkus.thread-pool.max-size=50
```

## Troubleshooting

### SSE Connection Closes Immediately
- Check server logs for `MCPController` errors
- Verify `RealmPlatform` is properly initialized
- Check Quarkus startup logs for SSE context issues

### Messages Not Received
- Ensure POST body is valid JSON
- Verify `Content-Type: application/json` header
- Check server logs for parsing errors

### Event Format Issues
- Each event must have: `data:`, `id:`, and blank line
- Use RestAssured or Raw HTTP to inspect stream format

## Next Steps

1. **Implement Event Broadcasting** — Send tool/resource updates to all SSE clients
2. **Add Message Routing** — Route messages to appropriate handlers
3. **Implement Heartbeat** — Send periodic keep-alive events
4. **Add Reconnection Logic** — Support client reconnection with state recovery
5. **Metrics Integration** — Track SSE connection count and message throughput
