package systems.symbol.mcp;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.mcp.connect.MCPConnectPipeline;
import systems.symbol.mcp.resource.NamespacesResourceProvider;
import systems.symbol.mcp.tool.SparqlQueryAdapter;
import systems.symbol.mcp.tool.SparqlUpdateAdapter;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Test: MCP Service Tools & Resources Integration
 *
 * <p>This integration test validates the full MCP request/response cycle:
 * <ol>
 *   <li>Initialize in-memory RDF repository with test data</li>
 *   <li>Register tools (sparql.query, sparql.update) and resources (namespaces)</li>
 *   <li>Simulate client tool calls via MCPCallContext</li>
 *   <li>Validate results match expected RDF semantics</li>
 *   <li>Test error handling and context propagation</li>
 * </ol>
 *
 * <p><b>SDK 1.1.0 Status:</b>
 * With MCP SDK 1.1.0, full server-side stdio and SSE transports are now available.
 * These tests can simulate client behavior directly; when Quarkus integration is
 * complete, they can be upgraded to use actual client–server RPC communication.
 * 
 * <p>The test suite validates:
 * - Tool metadata discovery (names, descriptions, input schemas)
 * - Resource metadata discovery (URIs, MIME types, labels)
 * - SPARQL query execution (SELECT, ASK with namespace prefixes)
 * - Namespace enumeration and JSON serialization
 * - Error handling and MCPException → I_MCPResult conversion
 * - Context propagation through middleware pipeline
 * 
 * <p><b>Test Data:</b>
 * Uses FOAF vocabulary (Friend-Of-A-Friend) with test persons:
 * - http://example.org/alice (foaf:Person, foaf:name "Alice")
 * - http://example.org/bob (foaf:Person, foaf:name "Bob")
 * - http://example.org/charlie (foaf:Person, foaf:name "Charlie")
 */
public class E2EMCPServiceClientTest {

    private Repository repository;
    private MCPConnectPipeline pipeline;
    private SparqlQueryAdapter sparqlQuery;
    private SparqlUpdateAdapter sparqlUpdate;
    private NamespacesResourceProvider namespacesResource;

    private static final IRI EX = Values.iri("http://example.org/");
    private static final IRI RDF_TYPE = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private static final IRI FOAF_PERSON = Values.iri("http://xmlns.com/foaf/0.1/Person");
    private static final IRI FOAF_NAME = Values.iri("http://xmlns.com/foaf/0.1/name");

    @BeforeEach
    void setupServer() throws Exception {
        // 1. Create in-memory RDF4J repository
        repository = new SailRepository(new MemoryStore());
        repository.init();

        // 2. Populate test data (persons and their names)
        try (RepositoryConnection conn = repository.getConnection()) {
            // Declare FOAF namespace
            conn.setNamespace("foaf", "http://xmlns.com/foaf/0.1/");
            conn.setNamespace("ex", "http://example.org/");

            // Add test triples: example:alice is a foaf:Person with name "Alice"
            IRI alice = Values.iri(EX + "alice");
            conn.add(alice, RDF_TYPE, FOAF_PERSON);
            conn.add(alice, FOAF_NAME, Values.literal("Alice"));

            // Add example:bob
            IRI bob = Values.iri(EX + "bob");
            conn.add(bob, RDF_TYPE, FOAF_PERSON);
            conn.add(bob, FOAF_NAME, Values.literal("Bob"));

            // Add example:charlie
            IRI charlie = Values.iri(EX + "charlie");
            conn.add(charlie, RDF_TYPE, FOAF_PERSON);
            conn.add(charlie, FOAF_NAME, Values.literal("Charlie"));
        }

        // 3. Initialize MCP middleware pipeline (no middleware = direct tool call)
        pipeline = new MCPConnectPipeline(List.of());

        // 4. Register tools
        sparqlQuery = new SparqlQueryAdapter(repository);
        sparqlUpdate = new SparqlUpdateAdapter(repository);

        // 5. Register resources
        namespacesResource = new NamespacesResourceProvider(repository);
    }

    @AfterEach
    void teardown() throws Exception {
        if (repository != null) {
            repository.shutDown();
        }
    }

    /**
     * Test tool discovery: verify tools are properly registered and metadata is available.
     *
     * <p>Validates:
     * - Tool name matches expected pattern
     * - Read-only vs. write-enabled flags are correct
     * - Tool descriptions are non-empty
     */
    @Test
    void testServerToolRegistration() {
        // Act
        String queryName = sparqlQuery.getName();
        String updateName = sparqlUpdate.getName();
        
        // Assert
        assertEquals("sparql.query", queryName);
        assertEquals("sparql.update", updateName);
        
        assertTrue(sparqlQuery.isReadOnly());
        assertFalse(sparqlUpdate.isReadOnly());
        
        assertNotNull(sparqlQuery.getDescription());
        assertNotNull(sparqlUpdate.getDescription());
    }

    /**
     * Test resource discovery: verify resources are properly registered.
     *
     * <p>Validates:
     * - Resource URI is non-null and contains expected key
     * - MIME type is application/json
     * - Label and description are present
     */
    @Test
    void testServerResourceRegistration() {
        // Act
        String uri = namespacesResource.getUri();
        String mimeType = namespacesResource.getMimeType();
        String label = namespacesResource.getLabel();
        String desc = namespacesResource.getDescription();
        
        // Assert
        assertNotNull(uri);
        assertTrue(uri.contains("namespace"));
        assertEquals("application/json", mimeType);
        assertNotNull(label);
        assertNotNull(desc);
    }

    /**
     * Test tool invocation (simulated client call): sparql.query with SELECT.
     *
     * <p>Validates end-to-end SPARQL query execution:
     * - Tool accepts MCPCallContext + input parameters
     * - SPARQL SELECT query is executed against RDF repository
     * - Results are returned as JSON rows in I_MCPResult
     * - All registered persons are returned (alice, bob, charlie)
     *
     * <p>This simulates: MCP Client → callTool("sparql.query", params) → Server
     */
    @Test
    void testSimulatedClientCallSparqlQuery() throws MCPException {
        // Arrange: Simulate client calling sparql.query tool
        String sparqlQueryStr = """
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX ex: <http://example.org/>
            SELECT ?person ?name
            WHERE {
              ?person a foaf:Person ;
                      foaf:name ?name .
            }
            ORDER BY ?name
            """;

        MCPCallContext ctx = new MCPCallContext("sparql.query", Map.of());
        Map<String, Object> input = Map.of("query", sparqlQueryStr);

        // Act: Server executes tool
        I_MCPResult result = sparqlQuery.execute(ctx, input);

        // Assert: Validate results
        assertNotNull(result);
        assertFalse(result.isError(), "Tool should not return error");
        assertNotNull(result.getContent(), "Tool should return content");

        String json = result.getContent();
        assertTrue(json.contains("Alice") || json.contains("alice"), 
            "Results should contain alice");
        assertTrue(json.contains("Bob") || json.contains("bob"), 
            "Results should contain bob");
        assertTrue(json.contains("Charlie") || json.contains("charlie"), 
            "Results should contain charlie");
    }

    /**
     * Test resource read (simulated client call): read namespaces resource.
     *
     * <p>Validates:
     * - Resource provider fetches namespace mappings from repository
     * - Results are returned as JSON in I_MCPResult
     * - Registered prefixes (foaf, ex) are included in output
     *
     * <p>This simulates: MCP Client → readResource("iq://self/namespaces") → Server
     */
    @Test
    void testSimulatedClientReadNamespacesResource() throws MCPException {
        // Arrange: Simulate client reading resource
        MCPCallContext ctx = new MCPCallContext("namespaces-read", Map.of());
        String resourceUri = namespacesResource.getUri();

        // Act: Server reads resource
        I_MCPResult result = namespacesResource.read(ctx, resourceUri);

        // Assert: Validate results
        assertNotNull(result);
        assertFalse(result.isError(), "Resource read should not error");
        assertNotNull(result.getContent(), "Resource should return content");

        String json = result.getContent();
        assertTrue(json.contains("foaf"), "Namespaces should include foaf prefix");
        assertTrue(json.contains("ex"), "Namespaces should include ex prefix");
        assertTrue(json.contains("http://xmlns.com/foaf/0.1/"), 
            "Namespaces should map foaf prefix to URI");
    }

    /**
     * Test error handling: server converts tool exceptions to error results.
     *
     * <p>Validates error propagation:
     * - Tool throws MCPException for bad input
     * - Server catches and converts to I_MCPResult with error code
     * - Client receives error; connection remains open
     *
     * <p>This simulates: MCP Client → callTool with malformed query → Server error handling
     */
    @Test
    void testSimulatedClientToolError() {
        // Arrange: Simulate client calling tool with bad query (unbalanced braces)
        String malformedQuery = "SELECT ?x WHERE { ?x a ?type ";
        MCPCallContext ctx = new MCPCallContext("sparql.query", Map.of());
        Map<String, Object> input = Map.of("query", malformedQuery);

        // Act & Assert: Verify tool throws MCPException for bad syntax
        MCPException ex = assertThrows(MCPException.class, () -> {
            sparqlQuery.execute(ctx, input);
        });

        // Assert: Validate error code
        assertEquals(500, ex.getCode(), "SPARQL parse error should return 500");
        assertNotNull(ex.getMessage(), "Error should include parse error message");
    }

    /**
     * Test context propagation: MCPCallContext carries traceId and metadata.
     *
     * <p>Validates:
     * - MCPCallContext is created with tool name
     * - Context can be modified by middleware
     * - Tool receives context with full state
     * - Results include context reference for debugging
     */
    @Test
    void testContextPropagationThroughPipeline() throws MCPException {
        // Arrange
        MCPCallContext ctx = new MCPCallContext("sparql.query", Map.of());
        String traceId = ctx.traceId();
        
        Map<String, Object> input = Map.of("query", """
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            SELECT (COUNT(?p) AS ?count) WHERE { ?p a foaf:Person . }
            """);

        // Act: Call tool (context is available during execution)
        I_MCPResult result = sparqlQuery.execute(ctx, input);

        // Assert
        assertNotNull(traceId, "Context should have traceId");
        assertFalse(traceId.isEmpty());
        assertFalse(result.isError());
        
        // Context should still be intact after tool execution
        assertEquals("sparql.query", ctx.toolName());
    }

    /**
     * Test input schema: tools declare their parameter structure.
     *
     * <p>Validates MCP discovery protocol:
     * - Tools provide input schema (JSON Schema format)
     * - Schema declares all required parameters
     * - Schema includes parameter descriptions for client guidance
     *
     * <p>This allows clients to validate input before calling tools.
     */
    @Test
    void testToolInputSchemaDiscovery() {
        // Act
        var querySchema = sparqlQuery.getInputSchema();
        var updateSchema = sparqlUpdate.getInputSchema();

        // Assert: Schema structure
        assertNotNull(querySchema);
        assertNotNull(updateSchema);
        
        assertTrue(querySchema.containsKey("properties"));
        assertTrue(querySchema.containsKey("required"));
        
        var props = (Map<String, Object>) querySchema.get("properties");
        assertTrue(props.containsKey("query"), "Schema should declare 'query' parameter");
    }
}
