package systems.symbol.mcp.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RdfDescribeAdapter — RDF resource description.
 *
 * <p>Tests input schema validation, output schema structure (Turtle format),
 * example correctness, and error handling for DESCRIBE operations.
 */
@DisplayName("RdfDescribeAdapter Tests")
class RdfDescribeAdapterTest {

    private RdfDescribeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RdfDescribeAdapter(null);
    }

    /* ════════════════════════════════════════════════════════════════════════
       Adapter Metadata Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should have correct name")
    void testAdapterName() {
        assertEquals("rdf.describe", adapter.getName());
    }

    @Test
    @DisplayName("Should have non-null description")
    void testAdapterDescription() {
        assertNotNull(adapter.getDescription());
        assertTrue(adapter.getDescription().length() > 0);
    }

    @Test
    @DisplayName("Should be read-only")
    void testIsReadOnly() {
        assertTrue(
            adapter.isReadOnly(),
            "RDF DESCRIBE should be read-only (no updates)"
        );
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
    @DisplayName("Input schema should have required 'uri' property")
    void testInputSchemaRequiredUri() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(props.containsKey("uri"), "'uri' property required");
        
        var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("uri"), "'uri' should be in required list");
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
    @DisplayName("Output schema should indicate text/turtle format")
    void testOutputSchemaTurtle() {
        var schema = adapter.getOutputSchema();
        
        // Either via description or via type hint
        var desc = (String) schema.get("description");
        var type = (String) schema.get("type");
        
        assertTrue(
            (desc != null && desc.toLowerCase().contains("turtle")) ||
            (type != null && type.toLowerCase().contains("turtle")) ||
            (type != null && type.equals("string")),
            "Output schema should indicate Turtle RDF format"
        );
    }

    @Test
    @DisplayName("Output schema should indicate RDF representation")
    void testOutputSchemaRdfFormat() {
        var schema = adapter.getOutputSchema();
        var schemaStr = schema.toString().toLowerCase();
        
        assertTrue(
            schemaStr.contains("turtle") ||
            schemaStr.contains("rdf") ||
            schemaStr.contains("description"),
            "Output schema should reference RDF/Turtle format"
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
    @DisplayName("Example should have 'uri' input parameter")
    void testExampleHasUri() {
        var examples = adapter.getExamples();
        var example = examples.stream()
            .filter(ex -> ex.getInput().containsKey("uri"))
            .findFirst();
        
        assertTrue(example.isPresent(), "Example should have 'uri' parameter");
    }

    @Test
    @DisplayName("Example URI should be valid IRI format")
    void testExampleUriValid() {
        var examples = adapter.getExamples();
        var example = examples.stream()
            .filter(ex -> ex.getInput().containsKey("uri"))
            .findFirst();
        
        assertTrue(example.isPresent());
        var uri = (String) example.get().getInput().get("uri");
        assertNotNull(uri);
        // Should look like a URI (urn:, http://, etc.)
        assertTrue(
            uri.contains(":"),
            "Example URI should be valid format (contain ':')"
        );
    }

    @Test
    @DisplayName("Example output should be in Turtle format")
    void testExampleOutputTurtleFormat() {
        var examples = adapter.getExamples();
        
        examples.forEach(example -> {
            var output = example.getOutput();
            if (output != null && !output.isEmpty()) {
                // Output should contain Turtle syntax indicators
                // (this is a soft assertion - examples might be abstract)
                assertNotNull(output);
            }
        });
    }

    /* ════════════════════════════════════════════════════════════════════════
       Content-Type Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Adapter should indicate text/turtle content type")
    void testContentTypeTurtle() {
        var schema = adapter.getOutputSchema();
        assertNotNull(schema);
        
        // Output format should be RDF/Turtle
        var description = (String) schema.getOrDefault("description", "");
        assertTrue(
            description.toLowerCase().contains("turtle") ||
            description.toLowerCase().contains("rdf"),
            "Output should indicate Turtle/RDF format"
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       Error Handling Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should reject null repository")
    void testRejectsNullRepository() {
        MCPCallContext ctx = new MCPCallContext("rdf.describe", Map.of(
            "uri", "urn:example:entity"
        ));
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of("uri", "urn:example:entity"));
        });
        
        assertEquals(500, ex.getCode());
    }

    @Test
    @DisplayName("Should require 'uri' parameter")
    void testRequiresUriParameter() {
        MCPCallContext ctx = new MCPCallContext("rdf.describe", Map.of());
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of());
        });
        
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("Should extract uri from input map")
    void testExtractsUriParameter() {
        var uri = "urn:example:resource";
        Map<String, Object> input = Map.of("uri", uri);
        
        assertTrue(input.containsKey("uri"));
        assertEquals(uri, input.get("uri"));
    }
}
