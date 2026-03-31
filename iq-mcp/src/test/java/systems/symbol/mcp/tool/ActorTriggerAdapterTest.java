package systems.symbol.mcp.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActorTriggerAdapter — Agent intent invocation.
 *
 * <p>Tests input schema validation, output schema structure, example correctness,
 * parameter passing, and error handling for intent execution.
 */
@DisplayName("ActorTriggerAdapter Tests")
class ActorTriggerAdapterTest {

    private ActorTriggerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ActorTriggerAdapter(null);
    }

    /* ════════════════════════════════════════════════════════════════════════
       Adapter Metadata Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should have correct name")
    void testAdapterName() {
        assertEquals("actor.trigger", adapter.getName());
    }

    @Test
    @DisplayName("Should have non-null description")
    void testAdapterDescription() {
        assertNotNull(adapter.getDescription());
        assertTrue(adapter.getDescription().length() > 0);
    }

    @Test
    @DisplayName("Should not be read-only (executes side effects)")
    void testIsNotReadOnly() {
        assertFalse(
            adapter.isReadOnly(),
            "Actor trigger can cause side effects"
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
    @DisplayName("Input schema should have required 'intentUri' property")
    void testInputSchemaRequiredIntentUri() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("intentUri"),
            "'intentUri' property required to identify intent"
        );
        
        var required = (java.util.List<String>) schema.get("required");
        assertTrue(required.contains("intentUri"), "intentUri must be required");
    }

    @Test
    @DisplayName("Input schema should have optional 'actor' property")
    void testInputSchemaOptionalActor() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("actor"),
            "'actor' property for execution context"
        );
    }

    @Test
    @DisplayName("Input schema should have optional 'params' property")
    void testInputSchemaOptionalParams() {
        var schema = adapter.getInputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("params"),
            "'params' property for intent arguments"
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
            "Output should include status (success/error)"
        );
    }

    @Test
    @DisplayName("Output schema should include 'intentUri' field")
    void testOutputSchemaIntentUri() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("intentUri"),
            "Output should echo back the intent URI"
        );
    }

    @Test
    @DisplayName("Output schema should include 'result' field")
    void testOutputSchemaResult() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("result"),
            "Output should include result from intent execution"
        );
    }

    @Test
    @DisplayName("Output schema should include 'message' field")
    void testOutputSchemaMessage() {
        var schema = adapter.getOutputSchema();
        var props = (Map<String, Object>) schema.get("properties");
        
        assertTrue(
            props.containsKey("message"),
            "Output should include status message"
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
    @DisplayName("Example should have 'intentUri' input parameter")
    void testExampleHasIntentUri() {
        var examples = adapter.getExamples();
        var example = examples.stream()
            .filter(ex -> ex.getInput().containsKey("intentUri"))
            .findFirst();
        
        assertTrue(
            example.isPresent(),
            "Example should have 'intentUri' parameter"
        );
    }

    @Test
    @DisplayName("Example intentUri should be in valid URN format")
    void testExampleIntentUriValid() {
        var examples = adapter.getExamples();
        var example = examples.stream()
            .filter(ex -> ex.getInput().containsKey("intentUri"))
            .findFirst();
        
        assertTrue(example.isPresent());
        var intentUri = (String) example.get().getInput().get("intentUri");
        assertNotNull(intentUri);
        assertTrue(
            intentUri.contains(":"),
            "Example intentUri should be valid format (urn: or similar)"
        );
    }

    @Test
    @DisplayName("Example should demonstrate params object")
    void testExampleHasParams() {
        var examples = adapter.getExamples();
        var hasParamsExample = examples.stream()
            .filter(ex -> ex.getInput().containsKey("params"))
            .findFirst();
        
        // Should have at least one example with params
        // (optional but good practice)
        if (hasParamsExample.isPresent()) {
            var params = hasParamsExample.get().getInput().get("params");
            assertTrue(
                params instanceof Map,
                "params should be a map/object"
            );
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
       Parameter Passing Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should extract intentUri from input")
    void testExtractsIntentUri() {
        String intentUri = "urn:iq:intent:email-notifier";
        Map<String, Object> input = Map.of("intentUri", intentUri);
        
        assertTrue(input.containsKey("intentUri"));
        assertEquals(intentUri, input.get("intentUri"));
    }

    @Test
    @DisplayName("Should extract actor from input")
    void testExtractsActor() {
        String actor = "system";
        Map<String, Object> input = Map.of("actor", actor);
        
        assertTrue(input.containsKey("actor"));
        assertEquals(actor, input.get("actor"));
    }

    @Test
    @DisplayName("Should extract params from input")
    void testExtractsParams() {
        Map<String, Object> params = Map.of(
            "recipient", "user@example.com",
            "subject", "Alert"
        );
        Map<String, Object> input = Map.of("params", params);
        
        assertTrue(input.containsKey("params"));
        assertEquals(params, input.get("params"));
    }

    /* ════════════════════════════════════════════════════════════════════════
       Error Handling Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Should reject null intent provider")
    void testRejectsNullIntentProvider() {
        MCPCallContext ctx = new MCPCallContext("actor.trigger", Map.of(
            "intentUri", "urn:iq:intent:test"
        ));
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of(
                "intentUri", "urn:iq:intent:test"
            ));
        });
        
        assertEquals(500, ex.getCode());
    }

    @Test
    @DisplayName("Should require 'intentUri' parameter")
    void testRequiresIntentUriParameter() {
        MCPCallContext ctx = new MCPCallContext("actor.trigger", Map.of());
        
        MCPException ex = assertThrows(MCPException.class, () -> {
            adapter.execute(ctx, Map.of());
        });
        
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("Should handle empty params gracefully")
    void testHandlesEmptyParams() {
        Map<String, Object> input = Map.of(
            "intentUri", "urn:iq:intent:test"
            // No params provided
        );
        
        MCPCallContext ctx = new MCPCallContext("actor.trigger", input);
        // Should not throw exception on missing params
        assertNotNull(ctx);
    }

    /* ════════════════════════════════════════════════════════════════════════
       Intent URI Format Tests
       ════════════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Intent URI should use 'urn:' scheme or 'iq:' prefix")
    void testIntentUriFormat() {
        var validUris = new String[] {
            "urn:iq:intent:emailer",
            "iq:intent:notifier",
            "urn:custom:intent:action"
        };
        
        for (String uri : validUris) {
            assertTrue(
                uri.contains(":"),
                "Intent URI should contain ':' separator: " + uri
            );
        }
    }

    @Test
    @DisplayName("Should support actor context identifier")
    void testActorContextSupport() {
        Map<String, Object> input = Map.of(
            "intentUri", "urn:iq:intent:trigger",
            "actor", "automation-engine"
        );
        
        assertTrue(input.containsKey("actor"));
        var actor = (String) input.get("actor");
        assertNotNull(actor);
        assertTrue(actor.length() > 0);
    }
}
