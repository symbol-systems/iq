package systems.symbol.mcp.fixtures;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import systems.symbol.mcp.I_MCPResource;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.resource.NamespacesResourceProvider;
import systems.symbol.mcp.resource.SchemaResourceProvider;
import systems.symbol.mcp.resource.VoidResourceProvider;
import systems.symbol.mcp.tool.ActorTriggerAdapter;
import systems.symbol.mcp.tool.RdfDescribeAdapter;
import systems.symbol.mcp.tool.SparqlQueryAdapter;
import systems.symbol.mcp.tool.SparqlUpdateAdapter;

import java.util.*;

/**
 * MCPTestBase — Common test infrastructure for MCP module tests.
 *
 * <p>Provides:
 * - In-memory RDF4J repository (MemoryStore)
 * - Pre-configured MCP tools (sparql.query, sparql.update, rdf.describe, actor.trigger)
 * - Pre-configured MCP resources (namespaces, schema, void, custom)
 * - Shared test data loading mechanism
 *
 * <p>Subclasses should:
 * 1. Extend MCPTestBase
 * 2. Call super.setUp() in their @BeforeEach
 * 3. Use getRepository() and getTools()/getResources() as needed
 * 4. Override loadTestData() to add domain-specific test data
 *
 * <p>Example:
 * <pre>{@code
 * public class MyMCPTest extends MCPTestBase {
 * @Override
 * protected void loadTestData(Repository repo) {
 * try (RepositoryConnection conn = repo.getConnection()) {
 * conn.add(Values.iri("http://example.org/alice"),
 *  Values.iri("http://xmlns.com/foaf/0.1/name"),
 *  Values.***REMOVED***("Alice"));
 * }
 * }
 *
 * @Test
 * void testSomething() {
 * // Use getTools(), getResources(), getRepository()
 * }
 * }
 * }</pre>
 */
public abstract class MCPTestBase {

protected Repository repository;
protected List<I_MCPTool> tools;
protected List<I_MCPResource> resources;

@BeforeEach
public void setUp() {
// Create in-memory repository
repository = new SailRepository(new MemoryStore());

// Load test-specific data
loadTestData(repository);

// Setup standard tools
tools = new ArrayList<>();
tools.add(new SparqlQueryAdapter(repository));
tools.add(new SparqlUpdateAdapter(repository));
tools.add(new RdfDescribeAdapter(repository));

// Setup ActorTriggerAdapter with no-op dispatcher
tools.add(ActorTriggerAdapter.noOp());

// Setup standard resources
resources = new ArrayList<>();
resources.add(new NamespacesResourceProvider(repository));
resources.add(new SchemaResourceProvider(repository));
resources.add(new VoidResourceProvider(repository));

// Allow subclasses to add custom tools/resources
registerCustomTools(tools);
registerCustomResources(resources);
}

/**
 * Override to load test-specific RDF data into the repository.
 * Called after repository initialization but before tool registration.
 */
protected void loadTestData(Repository repo) {
// Default: no-op, subclasses override
}

/**
 * Override to register custom test tools.
 * Called during setUp() after standard tools are registered.
 */
protected void registerCustomTools(List<I_MCPTool> toolList) {
// Default: no-op, subclasses override
}

/**
 * Override to register custom test resources.
 * Called during setUp() after standard resources are registered.
 */
protected void registerCustomResources(List<I_MCPResource> resourceList) {
// Default: no-op, subclasses override
}

/**
 * Provides access to the test repository for tool/resource operations.
 */
protected final Repository getRepository() {
return repository;
}

/**
 * Provides access to registered tools.
 */
protected final List<I_MCPTool> getTools() {
return tools;
}

/**
 * Provides access to registered resources.
 */
protected final List<I_MCPResource> getResources() {
return resources;
}

/**
 * Cleanup after test.
 */
protected void tearDown() {
if (repository != null) {
repository.shutDown();
}
}
}
