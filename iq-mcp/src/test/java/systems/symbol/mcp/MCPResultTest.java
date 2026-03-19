package systems.symbol.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

/**
 * Unit tests for MCPResult — immutable result envelope.
 */
class MCPResultTest {

@Test
void testSuccessResult() {
String content = "test content";
I_MCPResult result = MCPResult.okText(content);

assertFalse(result.isError());
assertEquals(content, result.getContent());
assertEquals(0, result.getErrorCode());
}

@Test
void testErrorResult() {
I_MCPResult result = MCPResult.error(403, "access forbidden");

assertTrue(result.isError());
assertEquals(403, result.getErrorCode());
assertTrue(result.getContent().contains("403"));
}

@Test
void testJsonResult() {
String json = "{\"result\":\"ok\"}";
I_MCPResult result = MCPResult.okJson(json);

assertFalse(result.isError());
assertEquals(json, result.getContent());
assertEquals("application/json", result.getMimeType());
}
}
