package systems.symbol.mcp.dynamic;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Dynamic tool registration via SPARQL INSERT.
 *
 * <p>Allows tools to be created on-the-fly by inserting RDF triples
 * into the {@code iq:catalog} named graph. MCPToolRegistry will automatically
 * discover and register them on next query.
 *
 * <p>Example: Register a custom tool via SPARQL:
 * <pre>
 * INSERT DATA {
 *   GRAPH &lt;iq:catalog&gt; {
 * &lt;iq:script/my-tool&gt; a iq:Script ;
 *   iq:name "my-tool" ;
 *   iq:description "My custom MCP tool" ;
 *   iq:sparql "SELECT ?item WHERE { ... }" ;
 *   iq:inputSchema "{\"type\":\"object\"}" .
 *   }
 * }
 * </pre>
 *
 * <p>The tool will be discovered by DynamicScriptBridge on next registry query.
 */
public class DynamicToolRegistration {

private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistration.class);

private final Repository repository;

public DynamicToolRegistration(Repository repository) {
this.repository = repository;
}

/**
 * Register a new tool dynamically via SPARQL INSERT.
 *
 * @param toolName the tool name (e.g., "my-query-tool")
 * @param description human-readable description
 * @param sparqlQuery the SPARQL query this tool executes
 * @param inputSchema JSON schema for tool inputs
 * @return true if registration succeeded, false otherwise
 */
public boolean registerToolViaSparql(String toolName, String description, String sparqlQuery, String inputSchema) {
if (toolName == null || toolName.trim().isEmpty()) {
log.warn("[DynamicToolRegistration] tool name cannot be empty");
return false;
}

try (RepositoryConnection conn = repository.getConnection()) {
ValueFactory vf = conn.getValueFactory();

// Build a safe IRI for the tool
IRI toolIri = vf.createIRI("iq:script/" + sanitizeForIRI(toolName));
IRI catalogGraph = vf.createIRI("iq:catalog");

// Add triples via SPARQL INSERT
String insert = String.format("""
PREFIX iq: <iq:>

INSERT DATA {
  GRAPH <iq:catalog> {
<%s> a iq:Script ;
  iq:name "%s" ;
  iq:description "%s" ;
  iq:sparql "%s" ;
  iq:inputSchema "%s" .
  }
}
""",
toolIri.stringValue(),
escapeStr(toolName),
escapeStr(description != null ? description : ""),
escapeStr(sparqlQuery),
escapeStr(inputSchema != null ? inputSchema : "{}"));

conn.prepareUpdate(insert).execute();
conn.commit();

log.info("[DynamicToolRegistration] registered tool via SPARQL: {}", toolName);
return true;
} catch (Exception ex) {
log.warn("[DynamicToolRegistration] failed to register tool {}: {}", toolName, ex.getMessage());
return false;
}
}

/**
 * Unregister a tool by removing it from the catalog.
 *
 * @param toolName the tool name to remove
 * @return true if removal succeeded, false otherwise
 */
public boolean unregisterTool(String toolName) {
if (toolName == null || toolName.trim().isEmpty()) {
return false;
}

try (RepositoryConnection conn = repository.getConnection()) {
String delete = String.format("""
PREFIX iq: <iq:>

DELETE {
  GRAPH <iq:catalog> {
?script a iq:Script ;
iq:name "%s" ;
?p ?o .
  }
}
WHERE {
  GRAPH <iq:catalog> {
?script a iq:Script ;
iq:name "%s" ;
?p ?o .
  }
}
""",
escapeStr(toolName),
escapeStr(toolName));

conn.prepareUpdate(delete).execute();
conn.commit();

log.info("[DynamicToolRegistration] unregistered tool: {}", toolName);
return true;
} catch (Exception ex) {
log.warn("[DynamicToolRegistration] failed to unregister tool {}: {}", toolName, ex.getMessage());
return false;
}
}

/**
 * Sanitize a string for use in an IRI.
 */
private String sanitizeForIRI(String s) {
return s.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase();
}

/**
 * Escape a string for use in SPARQL.
 */
private String escapeStr(String s) {
if (s == null) return "";
return s.replace("\\", "\\\\").replace("\"", "\\\"");
}

/**
 * Builder for fluent tool registration.
 */
public static class ToolBuilder {
private final String name;
private String description = "";
private String sparqlQuery = "";
private String inputSchema = "{}";
private final DynamicToolRegistration registry;

public ToolBuilder(String name, DynamicToolRegistration registry) {
this.name = name;
this.registry = registry;
}

public ToolBuilder description(String desc) {
this.description = desc;
return this;
}

public ToolBuilder sparql(String query) {
this.sparqlQuery = query;
return this;
}

public ToolBuilder schema(String schema) {
this.inputSchema = schema;
return this;
}

public boolean register() {
return registry.registerToolViaSparql(name, description, sparqlQuery, inputSchema);
}
}

/**
 * Create a builder for tool registration.
 */
public ToolBuilder builder(String toolName) {
return new ToolBuilder(toolName, this);
}
}
