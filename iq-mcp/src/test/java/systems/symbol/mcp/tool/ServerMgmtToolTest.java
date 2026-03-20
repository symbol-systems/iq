package systems.symbol.mcp.tool;

import org.junit.jupiter.api.Test;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;

import static org.junit.jupiter.api.Assertions.*;

class ServerMgmtToolTest {

    @Test
    void testToolMetadata() {
        ServerMgmtTool tool = new ServerMgmtTool();

        assertEquals("server.runtime", tool.getName());
        assertNotNull(tool.getDescription());
        assertNotNull(tool.getInputSchema());
    }

    @Test
    void testToolStartStopCycle() throws MCPException {
        ServerMgmtTool tool = new ServerMgmtTool();
        MCPCallContext ctx = new MCPCallContext("server.runtime", java.util.Map.of());

        var start = tool.execute(ctx, java.util.Map.of("runtime", "api", "action", "start"));
        assertNotNull(start);

        var health = tool.execute(ctx, java.util.Map.of("runtime", "api", "action", "health"));
        assertNotNull(health);

        var stop = tool.execute(ctx, java.util.Map.of("runtime", "api", "action", "stop"));
        assertNotNull(stop);
    }

    @Test
    void testToolInvalidAction() {
        ServerMgmtTool tool = new ServerMgmtTool();
        MCPCallContext ctx = new MCPCallContext("server.runtime", java.util.Map.of());

        MCPException ex = assertThrows(MCPException.class, () -> tool.execute(ctx, java.util.Map.of("runtime", "api", "action", "doomed")));
        assertTrue(ex.getMessage().contains("Unsupported action"));
    }
}
