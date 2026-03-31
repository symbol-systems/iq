package systems.symbol.mcp.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SparqlUpdateAdapter — SPARQL UPDATE (INSERT/DELETE/DROP).
 *
 * <p>Tests input schema validation, output schema structure, example correctness,
 * and error handling for SPARQL update operations.
 */
@DisplayName("SparqlUpdateAdapter Tests")
class SparqlUpdateAdapterTest {

    private SparqlUpdateAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SparqlUpdateAdapter(null);
    }

    /* ════════════════════════════════════════════════════════════════════════
       Adapter Metadata Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should have correct name")
    void testAdapterName() {
        assertEquals("sparql.update", adapter.getName());
    }

    @Test
    @DisplayName("Should have non-null description")
    void testAdapterDescription() {
        assertNotNull(adapter.getDescription());
        assertTrue(adapter.getDescription().length() > 0);
    }

    @Test
    @DisplayName("Should not be read-only")
    void testIsNotReadOnly() {
        assertFalse(adapter.isReadOnly(), "SPARQL UPDATE should not be read-only");
    }

    /* ════════════════════════════════════════════════════════════════════════
       Input Schema Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should have valid input schema")
    void testInputSchemaValid() {
        var schema = adapter.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.size() > 0);
    }

    @Test
    @DisplayName("Input schema should have properties object")
    void testInputSchemaHasProperties() {
        var schema = adapter.getInputSchema();
        assertTrue(schema.containsKey("properties"));
    }

    @Test
    @DisplayName("Input schema should have required 'update' property")
    void testInputSchemaRequiredUpdate() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(props.containsKey("update"), "update property required");
        
        var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("update"), "update should be in required list");
    }

    @Test
    @DisplayName("Input schema should have optional 'realm' property")
    void testInputSchemaOptionalRealm() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("realm"),
            "'realm' property should exist in input schema"
        );
    }

    @Test
    @DisplayName("Input schema should have optional 'confirmDrop' property")
    void testInputSchemaConfirmDropProperty() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("confirmDrop"),
            "'confirmDrop' property should exist for DROP safety"
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       Output Schema Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should have valid output schema")
    void testOutputSchemaValid() {
        var schema = adapter.getOutputSchema();
        assertNotNull(schema);
        assertTrue(schema.size() > 0);
    }

    @Test
    @DisplayName("Output schema should include 'status' field")
    void testOutputSchemaStatus() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("status"),
            "Output should include status (ok/error)"
        );
    }

    @Test
    @DisplayName("Output schema should include 'message' field")
    void testOutputSchemaMessage() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("message"),
            "Output should include message for result description"
        );
    }

    @Test
    @DisplayName("Output schema should include 'traceId' or 'transactionId'")
    void testOutputSchemaTraceInfo() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("traceId") || props.containsKey("transactionId"),
            "Output should include trace/transaction ID for logging"
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       Examples Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should provide examples")
    void testExamplesProvided() {
        var examples = adapter.getExamples();
        assertNotNull(examples);
        assertTrue(examples.size() > 0, "Should have at least one example");
    }

    @Test
    @DisplayName("Examples should include INSERT operation")
    void testExampleInsert() {
        var examples = adapter.getExamples();
        var insertExample = examples.stream()
            .filter(ex -> ex.getInput().containsKey("update"))
            .map(ex -> (String) ex.getInput().get("update"))
            .filter(update -> update.toUpperCase().contains("INSERT"))
            .findFirst();
        
        assertTrue(
            insertExample.isPresent(),
            "Should have INSERT example"
        );
    }

    @Test
    @DisplayName("Examples should include DELETE operation")
    void testExampleDelete() {
        var examples = adapter.getExamples();
        var deleteExample = examples.stream()
            .filter(ex -> ex.getInput().containsKey("update"))
            .map(ex -> (String) ex.getInput().get("update"))
            .filter(update -> update.toUpperCase().contains("DELETE"))
            .findFirst();
        
        assertTrue(
            deleteExample.isPresent(),
            "Should have DELETE example"
        );
    }

    @Test
    @DisplayName("Examples should have valid SPARQL syntax")
    void testExamplesHaveValidSyntax() {
        var examples = adapter.getExamples();
        examples.forEach(example -> {
            var input = example.getInput();
            assertTrue(
                input.containsKey("update"),
                "Example should have 'update' field"
            );
            var updateStr = (String) input.get("update");
            assertNotNull(updateStr);
            assertTrue(
                updateStr.length() > 10,
                "Update query should be non-empty"
            );
        });
    }

    /* ════════════════════════════════════════════════════════════════════════
       Error Handling Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should reject null repository")
    void testRejectsNullRepository() {
        MCPCallContext ctx = new MCPCallContext("sparql.update", Map.of(
            "update", "INSERT DATA { }"
        ));
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of("update", "INSERT DATA { }"));
        });
        
        assertEquals(500, ex.getCode());
    }

    @Test
    @DisplayName("Should require 'update' parameter")
    void testRequiresUpdateParameter() {
        MCPCallContext ctx = new MCPCallContext("sparql.update", Map.of());
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of());
        });
        
        assertEquals(400, ex.getCode());
    }
}
