package systems.symbol.cli;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import systems.symbol.IQConstants;
import systems.symbol.agent.AgentService;
import systems.symbol.agent.AgentService.ActorState;
import systems.symbol.platform.I_Contents;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.JarScriptCatalog;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;

/**
 * CLI Command: Boot the IQ realm and initialize all actors.
 * 
 * Discovers all Actor instances in the current realm and initializes them.
 * Supports waiting for actors to reach READY state.
 * 
 * SPARQL queries are loaded dynamically from JAR resources via JarScriptCatalog.
 * 
 * Usage:
 *   iq boot  # Initialize all actors
 *   iq boot --wait   # Wait for readiness (timeout: 30s)
 *   iq boot --wait --timeout=60  # Custom timeout
 * 
 * @author Symbol Systems
 */
@CommandLine.Command(name = "boot", description = "Booting " + I_Self.CODENAME + "...")
public class BootCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(BootCommand.class);

// IRI for SPARQL query to find all actors in current realm
// Maps to: /sparql/cli/builtin/boot-list-actors.sparql
private static final String QUERY_LIST_ACTORS_IRI = "urn:iq:script:cli:builtin:boot-list-actors";

private final AgentService agentService = new AgentService();

@CommandLine.Option(names = {"--wait"}, description = "Wait for actors to reach READY state")
private boolean waitForReady = false;

@CommandLine.Option(names = {"--timeout"}, description = "Timeout in seconds", 
defaultValue = "30")
private int timeout = IQConstants.BOOT_TIMEOUT_S;

@CommandLine.Option(names = {"--verbose"}, description = "Print detailed actor initialization info")
private boolean verbose = false;

public BootCommand(CLIContext context) throws IOException {
super(context);
}

/**
 * Executes boot sequence:
 * 1. Validate context is initialized
 * 2. Query for all actors in realm
 * 3. Log initialization status
 * 4. Optionally wait for actor readiness
 * 
 * @return boot result string, or null on failure
 */
@Override
public Object call() {
if (!context.isInitialized()) {
log.error("Context not initialized; cannot boot realm");
displayError("Error: iq context not initialized. Run 'iq init' first.");
return null;
}

IQStore iq = null;
try {
iq = context.newIQBase();
IRI realmIRI = context.getSelf();
log.info("Boot sequence starting for realm: {}", realmIRI);
display("Booting realm: " + realmIRI.getLocalName());

int actorCount = 0;
java.util.List<IRI> actorIRIs = new java.util.ArrayList<>();
try {
actorCount = initializeActors(iq, actorIRIs);
} catch (RuntimeException e) {
log.warn("Boot validation: no boot query available; assuming no actors to initialize", e);
display("No boot query found, skipping actor discovery.");
}

if (actorCount == 0) {
log.warn("No actors found in realm: {}", realmIRI);
display("  (no actors to initialize)");
return "boot:empty";
}

display("  Initialized " + actorCount + " actor(s)");
log.info("Boot complete: {} actor(s) initialized", actorCount);

if (waitForReady) {
display("  Waiting for actors to reach READY state...");
boolean allReady = waitForActorsReady(iq, actorIRIs, timeout);
if (allReady) {
display("  ✓ All actors READY");
return "boot:success:" + actorCount;
} else {
log.warn("Not all actors reached READY state within {} seconds", timeout);
displayError("  ✗ Timeout: Not all actors READY after " + timeout + "s");
return "boot:timeout:" + actorCount;
}
}

return "boot:success:" + actorCount;

} catch (RepositoryException e) {
log.error("Repository error during boot sequence", e);
displayError("Error: RDF repository error: " + e.getMessage());
return null;
} catch (Exception e) {
log.error("Unexpected error during boot sequence", e);
displayError("Error: " + e.getMessage());
return null;
} finally {
if (iq != null) {
try {
iq.close();
} catch (Exception e) {
log.warn("Failed to close IQ store", e);
}
}
}
}

/**
 * Initializes all actors found via SPARQL query.
 * Loads the query dynamically from JAR resources.
 *
 * @param iq The IQStore instance
 * @param actorIRIs List to populate with discovered actor IRIs
 * @return Number of actors initialized
 * @throws Exception if query execution fails
 */
private int initializeActors(IQStore iq, java.util.List<IRI> actorIRIs) throws Exception {
// Load SPARQL query dynamically from resources
String sparql = loadBootQuery();
if (sparql == null) {
log.warn("Boot actors query not found in resources, skipping actor initialization");
return 0;
}

if (verbose) {
log.debug("Executing boot query: {}", sparql);
}

RepositoryConnection conn = iq.getConnection();
try {
var tupleQuery = conn.prepareTupleQuery(sparql);
try (TupleQueryResult result = tupleQuery.evaluate()) {
int count = 0;
while (result.hasNext()) {
BindingSet binding = result.next();
IRI actorIRI = (IRI) binding.getBinding("actor").getValue();
actorIRIs.add(actorIRI);

// Mark actor as READY via AgentService
agentService.startActor(actorIRI);

log.info("Initializing actor: {}", actorIRI);
if (verbose) {
display("  - " + actorIRI.getLocalName());
}
count++;
}
return count;
}
} finally {
conn.close();
}
}

/**
 * Waits for all actors to reach READY state.
 *
 * @param iqThe IQStore instance
 * @param actorIRIs List of actor IRIs to check
 * @param timeout   Timeout in seconds
 * @return true if all actors reached READY within timeout, false otherwise
 * @throws Exception if an error occurs
 */
private boolean waitForActorsReady(IQStore iq, java.util.List<IRI> actorIRIs, int timeout) throws Exception {
long startTime = System.currentTimeMillis();
long endTime = startTime + (timeout * 1000L);

while (System.currentTimeMillis() < endTime) {
boolean allReady = checkActorsReady(actorIRIs);
if (allReady) {
return true;
}
Thread.sleep(500);  // Poll every 500ms
}

return false;
}

/**
 * Checks if all actors are in READY state.
 * Uses AgentService.getStatus() to check actual actor state.
 *
 * @param actorIRIs List of actor IRIs to check
 * @return true if all actors are READY, false otherwise
 */
private boolean checkActorsReady(java.util.List<IRI> actorIRIs) {
if (actorIRIs.isEmpty()) {
return true;
}

for (IRI actorIRI : actorIRIs) {
AgentService.ActorStatus status = agentService.getStatus(actorIRI);
if (status.state != ActorState.READY) {
log.trace("Actor {} is in state {}, waiting for READY", actorIRI, status.state);
return false;
}
}

log.debug("All {} actors have reached READY state", actorIRIs.size());
return true;
}

/**
 * Loads the boot actors query from JAR resources via JarScriptCatalog.
 *
 * @return The SPARQL query string, or null if not found
 */
private String loadBootQuery() {
try {
I_Contents catalog = new JarScriptCatalog();
IRI queryIRI = Values.iri(QUERY_LIST_ACTORS_IRI);
IRI sparqlMime = IQStore.vf.createIRI("urn:mimetype:application/sparql-query");

Literal ***REMOVED*** = catalog.getContent(queryIRI, sparqlMime);
if (***REMOVED*** != null) {
return ***REMOVED***.stringValue();
}
} catch (Exception e) {
log.warn("Error loading boot query from catalog: {}", e.getMessage(), e);
}
return null;
}

}

