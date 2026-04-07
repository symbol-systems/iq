package systems.symbol.mcp.dynamic;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamicScriptBridge — Phase 3.1: maps every {@code iq:Script} or
 * {@code iq:Query} in the catalog graph into a live MCP {@link I_MCPTool}.
 *
 * <p>When an operator adds a new {@code iq:Script} to the graph it
 * <em>instantly</em> appears as {@code script.<name>} in the MCP tool manifest
 * without any Java code changes.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>At startup (or on refresh) the bridge SPARQL-queries the
 *   {@code iq:catalog} named graph for all {@code iq:Script} resources.</li>
 *   <li>For each script it creates a {@link ScriptToolProxy} that:
 *   <ul>
 * <li>extracts SHACL / {@code iq:binding} properties as input schema,</li>
 * <li>delegates execution to the script runner lambda.</li>
 *   </ul>
 *   </li>
 * </ol>
 *
 * <h2>Catalog SPARQL</h2>
 * <pre>
 *   PREFIX iq: &lt;iq:&gt;
 *   SELECT ?script ?name ?description WHERE {
 * GRAPH &lt;iq:catalog&gt; {
 *   ?script a iq:Script ;
 *   iq:name?name ;
 *   iq:description ?description .
 * }
 *   }
 * </pre>
 */
public class DynamicScriptBridge {

private static final Logger log = LoggerFactory.getLogger(DynamicScriptBridge.class);

/**
 * Functional interface that executes a named script.
 * In production, wire this to {@code ScriptRunner} / {@code SPARQLExecutor}.
 */
@FunctionalInterface
public interface ScriptRunner {
String run(String scriptUri, Map<String, Object> bindings) throws Exception;
}

private final Repository   repository;
private final ScriptRunner runner;

public DynamicScriptBridge(Repository repository, ScriptRunner runner) {
this.repository = repository;
this.runner = runner;
}

/**
 * Discover all {@code iq:Script} resources in the catalog and return
 * them as ready-to-register {@link I_MCPTool} instances.
 *
 * @return list of dynamically generated MCP tools (may be empty)
 */
public List<I_MCPTool> discover() {
List<I_MCPTool> tools = new java.util.ArrayList<>();
try (RepositoryConnection conn = repository.getConnection()) {
String sparql = """
PREFIX iq: <iq:>
SELECT ?script ?name ?description WHERE {
GRAPH <iq:catalog> {
?script a iq:Script ;
iq:name?name ;
iq:description ?description .
}
}
""";
try (TupleQueryResult result = conn.prepareTupleQuery(sparql).evaluate()) {
while (result.hasNext()) {
var bs  = result.next();
String scriptUri= bs.getValue("script").stringValue();
String name = safeStr(bs, "name");
String desc = safeStr(bs, "description");
Map<String, Object> schema = discoverSchema(conn, scriptUri);
tools.add(new ScriptToolProxy("script." + name, desc, scriptUri, schema, runner));
log.info("[DynamicBridge] registered tool: script.{} ({})", name, scriptUri);
}
}
} catch (Exception ex) {
log.warn("[DynamicBridge] catalog discovery failed: {}", ex.getMessage());
}
return tools;
}

/** Extract SHACL / iq:binding properties for a script as JSON Schema properties. */
private Map<String, Object> discoverSchema(RepositoryConnection conn, String scriptUri) {
Map<String, Object> properties = new LinkedHashMap<>();
try {
String sparql = """
PREFIX iq:  <iq:>
PREFIX sh:  <http://www.w3.org/ns/shacl#>
SELECT ?param ?desc ?required WHERE {
GRAPH <iq:catalog> {
<%s> iq:binding ?param .
OPTIONAL { ?param sh:description ?desc }
OPTIONAL { ?param sh:minCount ?min . BIND(?min > 0 AS ?required) }
}
}
""".formatted(scriptUri);
try (TupleQueryResult r = conn.prepareTupleQuery(sparql).evaluate()) {
while (r.hasNext()) {
var bs= r.next();
String p  = bs.getValue("param").stringValue();
String d  = safeStr(bs, "desc");
properties.put(p, Map.of("type", "string", "description", d));
}
}
} catch (Exception ex) {
log.debug("[DynamicBridge] schema discovery failed for {}: {}", scriptUri, ex.getMessage());
}
return Map.of("type", "object", "properties", properties, "required", List.of());
}

private static String safeStr(org.eclipse.rdf4j.query.BindingSet bs, String var) {
var val = bs.getValue(var);
return val != null ? val.stringValue() : "";
}

/* ── inner proxy ─────────────────────────────────────────────────────── */

/**
 * ScriptToolProxy — adapts a single catalog script to the {@link I_MCPTool} SPI.
 */
static final class ScriptToolProxy implements I_MCPTool {

private final String  toolName;
private final String  description;
private final String  scriptUri;
private final Map<String, Object> schema;
private final ScriptRunner runner;

ScriptToolProxy(String toolName, String description,
String scriptUri, Map<String, Object> schema, ScriptRunner runner) {
this.toolName= toolName;
this.description = description;
this.scriptUri   = scriptUri;
this.schema  = schema;
this.runner  = runner;
}

@Override public String getName(){ return toolName; }
@Override public String getDescription() { return description; }
@Override public Map<String, Object> getInputSchema() { return schema; }
@Override public boolean isReadOnly(){ return true; }
@Override public int order() { return 200; } // dynamic tools come last

@Override
public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
try {
String result = runner.run(scriptUri, input);
return MCPResult.okJson(result);
} catch (Exception ex) {
throw MCPException.internal("Script '" + scriptUri + "' failed: " + ex.getMessage(), ex);
}
}
}
}
