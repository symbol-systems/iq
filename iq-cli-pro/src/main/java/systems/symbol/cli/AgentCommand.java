package systems.symbol.cli;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import systems.symbol.IQConstants;
import systems.symbol.platform.I_Contents;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.JarScriptCatalog;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CLI Command: Manage fleet of actors/agents.
 * 
 * Discovers all Actor instances in the current realm and manages their
 * lifecycle. Supports start, stop, status monitoring, and audit logging.
 * 
 * Usage:
 *   iq agent list  # List all actors with status
 *   iq agent status <IRI>  # Show status of specific actor
 *   iq agent start <IRI>   # Start an actor (transition to READY)
 *   iq agent stop <IRI># Stop an actor gracefully
 *   iq agent logs <IRI> [--limit]  # Show audit logs for actor
 * 
 * SPARQL queries are loaded dynamically from JAR resources via JarScriptCatalog.
 * 
 * @author Symbol Systems
 */
@CommandLine.Command(name = "agent", description = "Fleet and actor management")
public class AgentCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(AgentCommand.class);

// IRI for SPARQL queries to discover and manage actors
// Maps to: /sparql/cli/builtin/agent-*.sparql
private static final String QUERY_LIST_AGENTS_IRI = "urn:iq:script:cli:builtin:agent-list";
private static final String QUERY_AGENT_STATUS_IRI = "urn:iq:script:cli:builtin:agent-status";
private static final String QUERY_AUDIT_LOGS_IRI = "urn:iq:script:cli:builtin:agent-audit-logs";

@CommandLine.Parameters(index = "0", arity = "0..1", description = "action: list (default) | status | start | stop | logs")
String action = "list";

@CommandLine.Parameters(index = "1", arity = "0..1", description = "Actor IRI (required for status, start, stop, logs)")
String actorIRI = null;

@CommandLine.Option(names = {"--limit"}, description = "Limit number of results", defaultValue = "100")
int limit = 100;

@CommandLine.Option(names = {"--verbose"}, description = "Print detailed information")
boolean verbose = false;

public AgentCommand(CLIContext context) throws IOException {
super(context);
}

/**
 * Executes agent management command:
 * 1. Validate context is initialized
 * 2. Dispatch to appropriate handler based on action
 * 3. Return result or error
 * 
 * @return agent command result string, or null on failure
 */
@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
displayError("Error: iq context not initialized. Run 'iq init' first.");
log.error("Context not initialized; cannot manage agents");
return null;
}

try {
switch(action.toLowerCase()) {
case "list":
return listAgents();
case "status":
return statusAgent();
case "start":
return startAgent();
case "stop":
return stopAgent();
case "logs":
return auditLogs();
default:
displayError("Unknown action: " + action);
displayError("Available actions: list, status, start, stop, logs");
return null;
}
} catch (Exception e) {
log.error("iq.agent.error: {} - {}", action, e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
}
}

/**
 * Handle: iq agent list
 * Lists all actors/agents in the current realm with their status.
 */
private Object listAgents() throws Exception {
IQStore iq = context.newIQBase();
try {
display("Fleet Summary");
display("-".repeat(80));

String sparql = loadQuery(QUERY_LIST_AGENTS_IRI);
if (sparql == null) {
// Fallback: simple query without explicit SPARQL resource
sparql = "SELECT DISTINCT ?actor ?name ?state WHERE {\n" +
"  ?actor rdf:type <urn:iq:Actor> .\n" +
"  OPTIONAL { ?actor <http://www.w3.org/2000/01/rdf-schema#label> ?name } .\n" +
"  OPTIONAL { ?actor <urn:iq:state> ?state } .\n" +
"}";
if (verbose) {
log.debug("Using fallback agent list query");
}
}

if (verbose) {
log.debug("Executing agent list query");
}

RepositoryConnection conn = iq.getConnection();
try {
var tupleQuery = conn.prepareTupleQuery(sparql);
try (TupleQueryResult result = tupleQuery.evaluate()) {
int count = 0;
display(String.format("%-50s | %-30s | %-15s", "Actor IRI", "Name", "State"));
display("-".repeat(100));

while (result.hasNext()) {
BindingSet binding = result.next();

IRI actor = (IRI) binding.getBinding("actor").getValue();
String name = binding.getBinding("name") != null 
? binding.getBinding("name").getValue().stringValue() 
: "(unnamed)";
String state = binding.getBinding("state") != null 
? binding.getBinding("state").getValue().stringValue() 
: "UNKNOWN";

displayf("%-50s | %-30s | %-15s", 
actor.getLocalName(), 
name,
state);

if (verbose) {
display("IRI: " + actor.stringValue());
}

count++;
}

display("-".repeat(100));
display("Total: " + count + " agent(s)");

if (count == 0) {
log.warn("No agents found in realm");
display("  (no agents initialized)");
}

log.info("iq.agent.list: {} agent(s) found", count);
return "agents:listed:" + count;
}
} finally {
conn.close();
}
} catch (Exception e) {
log.error("Failed to list agents: {}", e.getMessage(), e);
displayError("Error listing agents: " + e.getMessage());
return null;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

/**
 * Handle: iq agent status <IRI>
 * Shows detailed status information for a specific actor.
 */
private Object statusAgent() throws Exception {
if (actorIRI == null || actorIRI.isEmpty()) {
displayError("Error: actor IRI required for 'status' action");
return null;
}

IQStore iq = context.newIQBase();
try {
display("Agent Status: " + actorIRI);
display("-".repeat(60));

String sparql = loadQuery(QUERY_AGENT_STATUS_IRI);
if (sparql == null) {
// Fallback query
sparql = "SELECT DISTINCT ?property ?value WHERE {\n" +
"  <" + actorIRI + "> ?property ?value .\n" +
"} LIMIT 50";
}

RepositoryConnection conn = iq.getConnection();
try {
var tupleQuery = conn.prepareTupleQuery(sparql);
tupleQuery.setBinding("actor", Values.iri(actorIRI));

try (TupleQueryResult result = tupleQuery.evaluate()) {
int propCount = 0;
while (result.hasNext()) {
BindingSet binding = result.next();
String property = binding.getBinding("property").getValue().stringValue();
String value = binding.getBinding("value").getValue().stringValue();

// Format property names nicely
String propName = property.substring(property.lastIndexOf("/") + 1)
.substring(property.lastIndexOf("#") + 1);

display(String.format("  %-25s: %s", propName, value));
propCount++;
}

if (propCount == 0) {
display("  (no properties found)");
}

display("-".repeat(60));
log.info("iq.agent.status: {} - {} properties", actorIRI, propCount);
return "agent:status:" + actorIRI;
}
} finally {
conn.close();
}
} catch (Exception e) {
log.error("Failed to get agent status: {}", e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

/**
 * Handle: iq agent start <IRI>
 * Transitions an actor to READY state.
 */
private Object startAgent() throws Exception {
if (actorIRI == null || actorIRI.isEmpty()) {
displayError("Error: actor IRI required for 'start' action");
return null;
}

try {
display("Starting agent: " + actorIRI);
display("  Transitioning to READY state...");

// Use AgentService to manage state transitions
systems.symbol.agent.AgentService agentService = 
new systems.symbol.agent.AgentService();

org.eclipse.rdf4j.model.IRI iri = org.eclipse.rdf4j.model.util.Values.iri(actorIRI);
boolean success = agentService.startActor(iri);

if (success) {
systems.symbol.agent.AgentService.ActorStatus status = agentService.getStatus(iri);
display("  ✓ Agent started successfully");
display("  State: " + status.state.label);
log.info("iq.agent.start: {}", actorIRI);
return "agent:started:" + actorIRI;
} else {
systems.symbol.agent.AgentService.ActorStatus status = agentService.getStatus(iri);
displayError("  ✗ Failed to start agent: " + status.lastError);
return null;
}
} catch (Exception e) {
log.error("Failed to start agent: {}", e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
}
}

/**
 * Handle: iq agent stop <IRI>
 * Stops an actor gracefully (saves checkpoint, closes connections).
 */
private Object stopAgent() throws Exception {
if (actorIRI == null || actorIRI.isEmpty()) {
displayError("Error: actor IRI required for 'stop' action");
return null;
}

try {
display("Stopping agent: " + actorIRI);

// Use AgentService to manage state transitions
systems.symbol.agent.AgentService agentService = 
new systems.symbol.agent.AgentService();

org.eclipse.rdf4j.model.IRI iri = org.eclipse.rdf4j.model.util.Values.iri(actorIRI);

display("  Saving checkpoint...");
boolean success = agentService.stopActor(iri);

if (success) {
systems.symbol.agent.AgentService.ActorStatus status = agentService.getStatus(iri);
display("  ✓ Checkpoint saved");
display("  Closing connections...");
display("  ✓ Connections closed");
display("  State: " + status.state.label);

log.info("iq.agent.stop: {}", actorIRI);
return "agent:stopped:" + actorIRI;
} else {
systems.symbol.agent.AgentService.ActorStatus status = agentService.getStatus(iri);
displayError("  ✗ Failed to stop agent: " + status.lastError);
return null;
}
} catch (Exception e) {
log.error("Failed to stop agent: {}", e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
}
}

/**
 * Handle: iq agent logs <IRI> [--limit]
 * Displays audit logs/events for a specific actor.
 */
private Object auditLogs() throws Exception {
if (actorIRI == null || actorIRI.isEmpty()) {
displayError("Error: actor IRI required for 'logs' action");
return null;
}

IQStore iq = context.newIQBase();
try {
display("Audit Logs: " + actorIRI);
display("Limit: " + limit + " entries");
display("-".repeat(80));

String sparql = loadQuery(QUERY_AUDIT_LOGS_IRI);
if (sparql == null) {
// Fallback query
sparql = "SELECT DISTINCT ?timestamp ?action ?result WHERE {\n" +
"  ?event <urn:mcp:audit:actor> <" + actorIRI + "> .\n" +
"  ?event <urn:mcp:audit:timestamp> ?timestamp .\n" +
"  ?event <urn:mcp:audit:action> ?action .\n" +
"  OPTIONAL { ?event <urn:mcp:audit:result> ?result } .\n" +
"} ORDER BY DESC(?timestamp) LIMIT " + limit;
}

RepositoryConnection conn = iq.getConnection();
try {
var tupleQuery = conn.prepareTupleQuery(sparql);
tupleQuery.setBinding("actor", Values.iri(actorIRI));

try (TupleQueryResult result = tupleQuery.evaluate()) {
int logCount = 0;
display(String.format("%-20s | %-30s | %-15s", "Timestamp", "Action", "Result"));
display("-".repeat(80));

while (result.hasNext() && logCount < limit) {
BindingSet binding = result.next();

String timestamp = binding.getBinding("timestamp") != null 
? binding.getBinding("timestamp").getValue().stringValue() 
: "(unknown)";
String action = binding.getBinding("action") != null 
? binding.getBinding("action").getValue().stringValue() 
: "(unknown)";
String result_val = binding.getBinding("result") != null 
? binding.getBinding("result").getValue().stringValue() 
: "N/A";

displayf("%-20s | %-30s | %-15s", 
timestamp.substring(Math.max(0, timestamp.length() - 20)),
action.length() > 30 ? action.substring(0, 27) + "..." : action,
result_val.length() > 15 ? result_val.substring(0, 12) + "..." : result_val);

logCount++;
}

display("-".repeat(80));

if (logCount == 0) {
display("  (no audit logs found)");
} else {
display("Total: " + logCount + " log entries");
}

log.info("iq.agent.logs: {} - {} entries", actorIRI, logCount);
return "logs:displayed:" + logCount;
}
} finally {
conn.close();
}
} catch (Exception e) {
log.error("Failed to retrieve audit logs: {}", e.getMessage(), e);
displayError("Error: " + e.getMessage());
return null;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

/**
 * Loads a query from JAR resources via JarScriptCatalog.
 *
 * @param queryIRI The IRI to load
 * @return The SPARQL query string, or null if not found
 */
private String loadQuery(String queryIRI) {
try {
I_Contents catalog = new JarScriptCatalog();
IRI iri = Values.iri(queryIRI);
IRI sparqlMime = IQStore.vf.createIRI("urn:mimetype:application/sparql-query");

Literal literal = catalog.getContent(iri, sparqlMime);
if (literal != null) {
return literal.stringValue();
}
} catch (Exception e) {
log.debug("Query not found in catalog ({}); using fallback query", queryIRI, e);
}
return null;
}

}
