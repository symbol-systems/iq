package systems.symbol.mcp.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCP resource providers.
 */
class ResourceProvidersTest {

    @Test
    void testNamespacesResourceProvider() {
        NamespacesResourceProvider provider = new NamespacesResourceProvider(null);
        
        assertNotNull(provider.getUri());
        assertTrue(provider.getUri().contains("namespace"));
        assertNotNull(provider.getLabel());
        assertNotNull(provider.getDescription());
        assertEquals("application/json", provider.getMimeType());
    }

    @Test
    void testVoidResourceProvider() {
        VoidResourceProvider provider = new VoidResourceProvider(null);
        
        assertNotNull(provider.getUri());
        assertTrue(provider.getUri().contains("void"));
        assertNotNull(provider.getLabel());
        assertNotNull(provider.getDescription());
    }

    @Test
    void testResourceUriMatching() {
        NamespacesResourceProvider provider = new NamespacesResourceProvider(null);
        String uri = provider.getUri();
        
        assertTrue(provider.matchesUri(uri));
        assertFalse(provider.matchesUri("urn:some:other:uri"));
    }
}
