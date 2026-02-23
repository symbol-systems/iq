package systems.symbol.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for MCP module interfaces.
 * Verifies that core contracts compile and are accessible.
 */
public class I_MCPAdapterTest {

@Test
public void testInterfacesExist() {
// Verify all core interfaces are loadable
assertNotNull(I_MCPAdapter.class);
assertNotNull(I_MCPResult.class);
assertNotNull(I_MCPToolManifest.class);
assertNotNull(I_MCPService.class);
}

@Test
public void testInterfaceContractsDefine() {
// Verify expected methods are defined
assertTrue(I_MCPAdapter.class.isInterface());
assertEquals(5, I_MCPAdapter.class.getDeclaredMethods().length);

assertTrue(I_MCPResult.class.isInterface());
assertEquals(7, I_MCPResult.class.getDeclaredMethods().length);

assertTrue(I_MCPToolManifest.class.isInterface());
assertEquals(8, I_MCPToolManifest.class.getDeclaredMethods().length);

assertTrue(I_MCPService.class.isInterface());
assertEquals(6, I_MCPService.class.getDeclaredMethods().length);
}
}
