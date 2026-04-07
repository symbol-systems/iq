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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DynamicAgentBridge — Phase 3.2: maps {@code iq:Agent} state transitions
 * into MCP tools.
 *
 * <p>For each deployed {@code iq:Agent} that has a named actor and discoverable
 * {@code iq:to} transitions this bridge creates tools of the form:
 * {@code agent.<actorName>.<intent>}.
 *
 * <p>Catalog query:
 * <pre>
 *   PREFIX iq: &lt;iq:&gt;
 *   SELECT ?agent ?actorName ?intent WHERE {
 * GRAPH &lt;iq:agents&gt; {
 *   ?agent a iq:Agent ;
 *  iq:name ?actorName ;
 *  iq:to   ?transition .
 *   ?transition iq:intent ?intent .
 * }
 *   }
 * </pre>
 */
public class DynamicAgentBridge {

private static final Logger log = LoggerFactory.getLogger(DynamicAgentBridge.class);

/**
 * Executes a state transition on a named agent.
 * Wire to {@code IntentAPI.trigger(realm, actor, intent, bindings)} in production.
 */
@FunctionalInterface
public interface AgentTransitionRunner {
String run(String agentUri, String actor, String intent, Map<String, Object> bindings) throws Exception;
}

private final Repository repository;
private final AgentTransitionRunner runner;

public DynamicAgentBridge(Repository repository, AgentTransitionRunner runner) {
this.repository = repository;
this.runner = runner;
}

/**
 * Discover all agent transitions and return them as live {@link I_MCPTool} instances.
 */
public List<I_MCPTool> discover() {
List<I_MCPTool> tools = new ArrayList<>();
try (RepositoryConnection conn = repository.getConnection()) {
String sparql = """
PREFIX iq: <iq:>
SELECT ?agent ?actorName ?intent WHERE {
GRAPH <iq:agents> {
?agent a iq:Agent ;
   iq:name ?actorName ;
   iq:to   ?transition .
?transition iq:intent ?intent .
}
}
""";
try (TupleQueryResult result = conn.prepareTupleQuery(sparql).evaluate()) {
while (result.hasNext()) {
varbs= result.next();
String agentUri  = bs.getValue("agent").stringValue();
String actorName = safeStr(bs, "actorName");
String intent= safeStr(bs, "intent");
String toolName  = "agent." + actorName + "." + intent;
tools.add(new AgentTransitionProxy(toolName, agentUri, actorName, intent, runner));
log.info("[DynamicAgentBridge] registered tool: {}", toolName);
}
}
} catch (Exception ex) {
log.warn("[DynamicAgentBridge] agent discovery failed: {}", ex.getMessage());
}
return tools;
}

private static String safeStr(org.eclipse.rdf4j.query.BindingSet bs, String var) {
var val = bs.getValue(var);
return val != null ? val.stringValue() : "";
}

/* ── inner proxy ─────────────────────────────────────────────────────── */

static final class AgentTransitionProxy implements I_MCPTool {

private final String  toolName;
private final String  agentUri;
private final String  actor;
private final String  intent;
private final AgentTransitionRunner runner;

AgentTransitionProxy(String toolName, String agentUri, String actor,
 String intent, AgentTransitionRunner runner) {
this.toolName  = toolName;
this.agentUri  = agentUri;
this.actor = actor;
this.intent= intent;
this.runner= runner;
}

@Override public String getName()  { return toolName; }
@Override public int order()   { return 300; }
@Override public boolean isReadOnly() { return false; }

@Override
public String getDescription() {
return "Execute the '" + intent + "' transition on agent '" + actor + "'. " +
   "Agent URI: " + agentUri;
}

@Override
public Map<String, Object> getInputSchema() {
return Map.of(
"type", "object",
"properties", Map.of(
"bindings", Map.of("type", "object", "description", "Optional transition parameter bindings.")
),
"required", List.of()
);
}

@Override
public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
@SuppressWarnings("unchecked")
Map<String, Object> bindings = input.containsKey("bindings")
? (Map<String, Object>) input.get("bindings") : Map.of();
try {
String result = runner.run(agentUri, actor, intent, bindings);
return MCPResult.okJson(result);
} catch (Exception ex) {
throw MCPException.internal("Agent transition '" + toolName + "' failed: " + ex.getMessage(), ex);
}
}
}
}
