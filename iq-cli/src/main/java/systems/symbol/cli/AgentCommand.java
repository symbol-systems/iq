package systems.symbol.cli;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import systems.symbol.io.Display;
import systems.symbol.platform.AgentService;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.sparql.JarScriptCatalog;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import picocli.CommandLine;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "agent", description = "Agent transition management: list and trigger agent transitions")
public class AgentCommand extends AbstractCLICommand {

// IRI for SPARQL query to list agent transitions
// Maps to: /sparql/cli/builtin/agent-list-transitions.sparql
private static final String QUERY_LIST_TRANSITIONS_IRI = "urn:iq:script:cli:builtin:agent-list-transitions";

@CommandLine.Option(names = "--list", description = "List available agent transitions")
boolean list = false;

@CommandLine.Option(names = "--actor", description = "Restrict by actor name")
String actor;

@CommandLine.Option(names = "--intent", description = "Restrict by intent name")
String intent;

@CommandLine.Option(names = "--agent", description = "Restrict by agent IRI")
String agent;

@CommandLine.Option(names = "--trigger", description = "Trigger a matched agent transition")
boolean trigger = false;

public AgentCommand(CLIContext context) throws IOException {
super(context);
}

@Override
protected Object doCall() throws Exception {
if (!context.isInitialized()) {
throw new CLIException("Workspace is not initialized. run `iq init` first.");
}

if (list || !trigger) {
listTransitions();
}

if (trigger) {
if ((actor == null || actor.isBlank()) || (intent == null || intent.isBlank())) {
throw new CLIException("--trigger requires --actor and --intent");
}
triggerTransition(actor, intent);
}

return 0;
}

private void listTransitions() {
try (RepositoryConnection conn = context.getRepository().getConnection()) {
IQStore iq = new IQConnection(context.getSelf(), conn);
SPARQLMapper mapper = new SPARQLMapper(iq);

// Load SPARQL query dynamically from resources
String q = loadListTransitionsQuery();
if (q == null) {
display("Error: Failed to load transitions query from resources");
return;
}

List<Map<String, Object>> transitions = mapper.query(q, null);
if (agent != null && !agent.isBlank()) {
transitions.removeIf(m -> !agent.equals(m.get("agent")));
}
if (actor != null && !actor.isBlank()) {
transitions.removeIf(m -> !actor.equals(m.get("actorName")));
}
if (intent != null && !intent.isBlank()) {
transitions.removeIf(m -> !intent.equals(m.get("intent")));
}

if (transitions.isEmpty()) {
display("No agent transitions found.");
} else {
Display.display(transitions);
}

} catch (Exception ex) {
display("Failed to list transitions: " + ex.getMessage());
}
}

private void triggerTransition(String actor, String intent) {
try (RepositoryConnection conn = context.getRepository().getConnection()) {
IRI actorIRI = SimpleValueFactory.getInstance().createIRI(actor);
AgentService service = new AgentService(actorIRI, conn, context.getKernelContext().getSecrets(), new SimpleBindings());

if (service.getAgent() == null) {
display("Agent not found: " + actor);
return;
}

Resource before = service.getAgent().getStateMachine().getState();
Resource intentResource = SimpleValueFactory.getInstance().createIRI(intent);
Resource after = service.next(intentResource);

display("Agent transition executed: actor=" + actor + " intent=" + intent);
display("before=" + before + " after=" + after);
} catch (Exception e) {
display("Agent transition failed: " + e.getMessage());
log.error("Agent transition error", e);
}
}

/**
 * Loads the list transitions query from JAR resources via JarScriptCatalog.
 *
 * @return The SPARQL query string, or null if not found
 */
private String loadListTransitionsQuery() {
try {
I_Contents catalog = new JarScriptCatalog();
IRI queryIRI = Values.iri(QUERY_LIST_TRANSITIONS_IRI);
IRI sparqlMime = IQStore.vf.createIRI("urn:mimetype:application/sparql-query");

Literal literal = catalog.getContent(queryIRI, sparqlMime);
if (literal != null) {
return literal.stringValue();
}
} catch (Exception e) {
log.warn("Error loading transitions query from catalog: {}", e.getMessage());
}
return null;
}
}

