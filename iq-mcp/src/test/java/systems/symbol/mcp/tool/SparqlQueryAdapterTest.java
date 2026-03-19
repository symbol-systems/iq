package systems.symbol.mcp.tool;

import org.junit.jupiter.api.Test;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SparqlQueryAdapter — SPARQL SELECT read-only queries.
 */
class SparqlQueryAdapterTest {

    @Test
    void testAdapterMetadata() {
        SparqlQueryAdapter adapter = new SparqlQueryAdapter(null);
        
        assertEquals("sparql.query", adapter.getName());
        assertNotNull(adapter.getDescription());
        assertNotNull(adapter.getInputSchema());
    }

    @Test
    void testInputSchemaContainsQueryParameter() {
        SparqlQueryAdapter adapter = new SparqlQueryAdapter(null);
        var schema = adapter.getInputSchema();
        
        assertNotNull(schema);
        assertTrue(schema.containsKey("properties"));
        var props = (java.util.Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("query"));
    }

    @Test
    void testAdapterRejectsNullRepository() {
        SparqlQueryAdapter adapter = new SparqlQueryAdapter(null);
        MCPCallContext ctx = new MCPCallContext("sparql.query", java.util.Map.of(
            "query", "SELECT ?x WHERE { ?x a ?type }"
        ));
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, java.util.Map.of(
                "query", "SELECT ?x WHERE { ?x a ?type }"
            ));
        });
        
        assertEquals(500, ex.getCode());
    }
}
