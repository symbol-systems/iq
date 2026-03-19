package systems.symbol.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for MCPCallContext — the mutable envelope threaded through the middleware pipeline.
 */
class MCPCallContextTest {

@Test
void testContextInitialization() {
MCPCallContext ctx = new MCPCallContext("sparql.query", Map.of());

assertNotNull(ctx.traceId());
assertNotNull(ctx.startTime());
assertEquals("sparql.query", ctx.toolName());
assertFalse(ctx.isAuthorised());
assertNull(ctx.principal());
}

@Test
void testAttributeStore() {
MCPCallContext ctx = new MCPCallContext("tool1", Map.of());

assertFalse(ctx.has("custom"));
ctx.set("custom", "value");
assertTrue(ctx.has("custom"));
assertEquals("value", ctx.get("custom"));
}

@Test
void testPrincipalStorage() {
MCPCallContext ctx = new MCPCallContext("tool1", Map.of());

ctx.set(MCPCallContext.KEY_PRINCIPAL, "alice");
assertEquals("alice", ctx.principal());
}

@Test
void testRawInputImmutability() {
Map<String, Object> input = new HashMap<>();
input.put("query", "SELECT * WHERE { ?s ?p ?o }");

MCPCallContext ctx = new MCPCallContext("tool1", input);
Map<String, Object> retrieved = ctx.rawInput();

// Attempt to modify retrieved map should not affect context
assertThrows(UnsupportedOperationException.class, () -> {
retrieved.put("query", "MODIFIED");
});

assertEquals("SELECT * WHERE { ?s ?p ?o }", ctx.rawInput().get("query"));
}

@Test
void testAttributesReadOnlyView() {
MCPCallContext ctx = new MCPCallContext("tool1", Map.of());
ctx.set("key1", "value1");

var attrs = ctx.attributes();
assertThrows(UnsupportedOperationException.class, () -> {
attrs.put("key2", "value2");
});
}
}
