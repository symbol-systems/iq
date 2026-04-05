package systems.symbol.mcp.tool;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * RdfDescribeAdapter — convenience wrapper for SPARQL DESCRIBE.
 *
 * <p>Takes an entity URI and returns its full Turtle representation.
 * Saves the LLM from having to compose a DESCRIBE query manually.
 *
 * <p>Tool name: {@value systems.symbol.mcp.MCP_NS#TOOL_RDF_DESCRIBE}
 */
public class RdfDescribeAdapter implements I_MCPTool {

private static final Logger log = LoggerFactory.getLogger(RdfDescribeAdapter.class);
private final Repository repository;

public RdfDescribeAdapter(Repository repository) { this.repository = repository; }

@Override public String getName()   { return "rdf.describe"; }
@Override public boolean isReadOnly()   { return true; }
@Override public int defaultRateLimit() { return 50; }
@Override public int order(){ return 30; }

@Override
public String getDescription() {
return """
   Retrieve the complete RDF description of an entity (SPARQL DESCRIBE).
   Returns all known triples where the entity appears as subject or object.
   Use this to inspect a specific resource by URI.
   """;
}

@Override
public Map<String, Object> getInputSchema() {
return Map.of(
"type", "object",
"properties", Map.of(
"uri",   Map.of("type", "string", "description", "The IRI of the entity to describe."),
"realm", Map.of("type", "string", "description", "Optional realm to query.")
),
"required", List.of("uri")
);
}

@Override
public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
String uri = (String) input.get("uri");
if (uri == null || uri.isBlank()) throw MCPException.badRequest("'uri' is required");

log.debug("[rdf.describe] uri={} [trace={}]", uri, ctx.traceId());

// Load DESCRIBE template from resource and substitute URI parameter
String template = loadSparqlTemplate("sparql/rdf-describe.sparql");
String describe = template.replace("{uri}", uri);

try (RepositoryConnection conn = repository.getConnection();
 GraphQueryResult result   = conn.prepareGraphQuery(describe).evaluate()) {
var model = QueryResults.asModel(result);
if (model.isEmpty()) return MCPResult.notFound(uri);
StringWriter sw = new StringWriter();
Rio.write(model, sw, RDFFormat.TURTLE);
return MCPResult.ok(sw.toString(), "text/turtle");
} catch (Exception ex) {
throw MCPException.internal("DESCRIBE failed for <" + uri + ">: " + ex.getMessage(), ex);
}
}

/**
 * Load SPARQL query template from classpath resource.
 * @param resourcePath path relative to classpath (e.g., "sparql/rdf-describe.sparql")
 * @return the query template text
 */
private static String loadSparqlTemplate(String resourcePath) {
try {
var resource = RdfDescribeAdapter.class.getClassLoader().getResourceAsStream(resourcePath);
if (resource == null) {
throw new IllegalArgumentException("Resource not found: " + resourcePath);
}
return new String(resource.readAllBytes(), StandardCharsets.UTF_8)
.replaceAll("^#.*$", "")  // Remove comment lines
.replaceAll("\\s+", " ")  // Normalize whitespace
.trim();
} catch (Exception ex) {
throw new RuntimeException("Failed to load SPARQL template from " + resourcePath, ex);
}
}
}
