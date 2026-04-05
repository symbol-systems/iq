package systems.symbol.mcp.persistence;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * MCPToolRegistry Persistence — Load/save tool definitions from/to RDF graph.
 *
 * <p>Tools can be persisted in the {@code mcp:tools} named graph as RDF resources:
 * <pre>
 * PREFIX mcp: &lt;urn:mcp:&gt;
 * &lt;urn:mcp:tool/sparql.query&gt; a mcp:Tool ;
 *   mcp:name "sparql.query" ;
 *   mcp:description "Execute SPARQL SELECT queries" ;
 *   mcp:enabled true ;
 *   mcp:readOnly true ;
 *   mcp:defaultRateLimit 100 ;
 *   mcp:order 10 .
 * </pre>
 *
 * <p>At startup, MCPToolRegistry loads these definitions and uses them to:
 * - Register external tools (webhooks, user-defined scripts)
 * - Override default tool properties (rate limits, descriptions)
 * - Enable/disable tools per realm or environment
 *
 * <p>This enables dynamic tool management without code redeployment.
 */
public class MCPToolPersistence {

private static final Logger log = LoggerFactory.getLogger(MCPToolPersistence.class);

private static final String TOOLS_GRAPH = "urn:mcp:tools";
private static final String TOOL_NS = "urn:mcp:";

private final Repository repository;

public MCPToolPersistence(Repository repository) {
this.repository = repository;
}

/**
 * Load tool configuration from the mcp:tools RDF graph.
 *
 * @return Map of tool name → configuration properties
 */
public Map<String, ToolConfiguration> loadToolConfigurations() {
Map<String, ToolConfiguration> configs = new HashMap<>();

try (RepositoryConnection conn = repository.getConnection()) {
String sparql = """
PREFIX mcp: <urn:mcp:>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?toolUri ?name ?description ?enabled ?readOnly ?rateLimit ?order WHERE {
GRAPH <urn:mcp:tools> {
?toolUri a mcp:Tool ;
 mcp:name ?name .
OPTIONAL { ?toolUri mcp:description ?description }
OPTIONAL { ?toolUri mcp:enabled ?enabled }
OPTIONAL { ?toolUri mcp:readOnly ?readOnly }
OPTIONAL { ?toolUri mcp:defaultRateLimit ?rateLimit }
OPTIONAL { ?toolUri mcp:order ?order }
}
} ORDER BY ?order
""";

var result = conn.prepareTupleQuery(sparql).evaluate();
while (result.hasNext()) {
var bs = result.next();
String name = bs.getValue("name").stringValue();
var config = new ToolConfiguration(name);

if (bs.hasBinding("description")) {
config.description = bs.getValue("description").stringValue();
}
if (bs.hasBinding("enabled")) {
config.enabled = Boolean.parseBoolean(bs.getValue("enabled").stringValue());
}
if (bs.hasBinding("readOnly")) {
config.readOnly = Boolean.parseBoolean(bs.getValue("readOnly").stringValue());
}
if (bs.hasBinding("rateLimit")) {
config.defaultRateLimit = Integer.parseInt(bs.getValue("rateLimit").stringValue());
}
if (bs.hasBinding("order")) {
config.order = Integer.parseInt(bs.getValue("order").stringValue());
}

configs.put(name, config);
log.info("[MCPToolPersistence] loaded tool config: {}", name);
}
} catch (Exception ex) {
log.warn("[MCPToolPersistence] failed to load tool configurations: {}", ex.getMessage());
}

return configs;
}

/**
 * Save a tool configuration to the mcp:tools RDF graph.
 */
public void saveToolConfiguration(String toolName, ToolConfiguration config) {
try (RepositoryConnection conn = repository.getConnection()) {
ValueFactory vf = conn.getValueFactory();
IRI toolUri = vf.createIRI(TOOL_NS + "tool/" + toolName);
IRI toolType = vf.createIRI(TOOL_NS + "Tool");
IRI toolsGraph = vf.createIRI(TOOLS_GRAPH);

conn.add(toolUri, RDF.TYPE, toolType, toolsGraph);
conn.add(toolUri, vf.createIRI(TOOL_NS + "name"), vf.createLiteral(toolName), toolsGraph);

if (config.description != null) {
conn.add(toolUri, vf.createIRI(TOOL_NS + "description"), vf.createLiteral(config.description), toolsGraph);
}
conn.add(toolUri, vf.createIRI(TOOL_NS + "enabled"), vf.createLiteral(config.enabled), toolsGraph);
conn.add(toolUri, vf.createIRI(TOOL_NS + "readOnly"), vf.createLiteral(config.readOnly), toolsGraph);
conn.add(toolUri, vf.createIRI(TOOL_NS + "defaultRateLimit"), vf.createLiteral(config.defaultRateLimit), toolsGraph);
conn.add(toolUri, vf.createIRI(TOOL_NS + "order"), vf.createLiteral(config.order), toolsGraph);

log.info("[MCPToolPersistence] saved tool config: {}", toolName);
} catch (Exception ex) {
log.warn("[MCPToolPersistence] failed to save tool configuration: {}", ex.getMessage());
}
}

/**
 * Tool configuration container.
 */
public static class ToolConfiguration {
public String name;
public String description;
public boolean enabled = true;
public boolean readOnly = false;
public int defaultRateLimit = 0;  // 0 = unlimited
public int order = 50;  // default order

public ToolConfiguration(String name) {
this.name = name;
}
}
}
