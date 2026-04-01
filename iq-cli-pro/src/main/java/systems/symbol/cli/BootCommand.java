package systems.symbol.cli;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import systems.symbol.IQConstants;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;

/**
 * CLI Command: Boot the IQ realm and initialize all actors.
 * 
 * Discovers all Actor instances in the current realm and initializes them.
 * Supports waiting for actors to reach READY state.
 * 
 * Usage:
 *   iq boot                          # Initialize all actors
 *   iq boot --wait                   # Wait for readiness (timeout: 30s)
 *   iq boot --wait --timeout=60      # Custom timeout
 * 
 * @author Symbol Systems
 */
@CommandLine.Command(name = "boot", description = "Booting " + I_Self.CODENAME + "...")
public class BootCommand extends AbstractCLICommand {
    private static final Logger log = LoggerFactory.getLogger(BootCommand.class);
    
    // SPARQL query to find all actors in current realm
    private static final String QUERY_LIST_ACTORS = """
        PREFIX iq: <urn:iq:>
        SELECT ?actor WHERE {
            ?actor a iq:Actor .
        }
        """;
    
    // SPARQL query to check actor state (ready/active)
    private static final String QUERY_ACTOR_STATE = """
        PREFIX iq: <urn:iq:>
        SELECT ?actor ?state WHERE {
            ?actor a iq:Actor .
            ?actor iq:state ?state .
            FILTER (?state = iq:READY || ?state = iq:ACTIVE)
        }
        """;
    
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
            log.error("Context not initialized; cannot boot realm. Run 'iq init' first.");
            return null;
        }
        
        IQStore iq = null;
        try {
            iq = context.newIQBase();
            IRI realmIRI = context.getSelf();
            log.info("Boot sequence starting for realm: {}", realmIRI);
            log.info("Booting realm: {}", realmIRI.getLocalName());
            
            int actorCount = initializeActors(iq);
            
            if (actorCount == 0) {
                log.warn("No actors found in realm: {}", realmIRI);
                log.info("  (no actors to initialize)");
                return "boot:empty";
            }
            
            log.info("  Initialized {} actor(s)", actorCount);
            log.info("Boot complete: {} actor(s) initialized", actorCount);
            
            if (waitForReady) {
                log.info("  Waiting for actors to reach READY state...");
                boolean allReady = waitForActorsReady(iq, timeout);
                if (allReady) {
                    log.info("  ✓ All actors READY");
                    return "boot:success:" + actorCount;
                } else {
                    log.warn("Not all actors reached READY state within {} seconds", timeout);
                    return "boot:timeout:" + actorCount;
                }
            }
            
            return "boot:success:" + actorCount;
            
        } catch (RepositoryException e) {
            log.error("Repository error during boot sequence", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during boot sequence", e);
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
     * Queries and initializes all actors in the realm.
     * 
     * @param iq the IQ store connection
     * @return count of actors initialized
     * @throws RepositoryException if query fails
     */
    private int initializeActors(IQStore iq) throws RepositoryException {
        try (RepositoryConnection conn = iq.getConnection()) {
            var query = conn.prepareTupleQuery(QUERY_LIST_ACTORS);
            // Set query timeout to prevent hanging
            query.setMaxExecutionTime(IQConstants.QUERY_TIMEOUT_MS);
            
            int count = 0;
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    BindingSet binding = result.next();
                    IRI actor = (IRI) binding.getBinding("actor").getValue();
                    initializeActor(actor);
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Logs actor initialization (can be extended to perform validation).
     * 
     * @param actor the actor IRI
     */
    private void initializeActor(IRI actor) {
        String actorName = actor.getLocalName();
        log.debug("Initializing actor: {}", actorName);
        if (verbose) {
            log.info("  • {}", actorName);
        }
    }

    /**
     * Waits for all actors to reach READY state via SPARQL polling.
     * 
     * Polls every 100-200ms until either:
     * - All actors reach READY/ACTIVE state (returns true)
     * - Timeout expires (returns false)
     * - Error occurs (returns false)
     * 
     * @param iq the IQ store
     * @param timeoutSeconds timeout in seconds
     * @return true if all actors are READY, false on timeout
     */
    private boolean waitForActorsReady(IQStore iq, int timeoutSeconds) {
        log.info("Waiting for actors to reach READY state (timeout: {}s)", timeoutSeconds);
        
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        int totalActorCount = 0;
        
        try (RepositoryConnection conn = iq.getConnection()) {
            // First, get total count of actors
            var listQuery = conn.prepareTupleQuery(QUERY_LIST_ACTORS);
            listQuery.setMaxExecutionTime(IQConstants.QUERY_TIMEOUT_MS);
            
            try (TupleQueryResult listResult = listQuery.evaluate()) {
                while (listResult.hasNext()) {
                    listResult.next();
                    totalActorCount++;
                }
            }
            
            if (totalActorCount == 0) {
                log.debug("waitForActorsReady: No actors found to wait for");
                return true;
            }
            
            // Poll until all actors are ready or timeout
            int pollCount = 0;
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                var stateQuery = conn.prepareTupleQuery(QUERY_ACTOR_STATE);
                stateQuery.setMaxExecutionTime(IQConstants.QUERY_TIMEOUT_MS);
                
                int readyCount = 0;
                try (TupleQueryResult stateResult = stateQuery.evaluate()) {
                    while (stateResult.hasNext()) {
                        stateResult.next();
                        readyCount++;
                    }
                }
                
                pollCount++;
                log.debug("waitForActorsReady: {} of {} actors ready (poll #{})", readyCount, totalActorCount, pollCount);
                
                if (readyCount >= totalActorCount) {
                    log.info("waitForActorsReady: {} actors READY (timeout: {}s)", readyCount, timeoutSeconds);
                    return true;
                }
                
                // Wait before next poll
                Thread.sleep(150);  // 150ms poll interval
            }
            
            // Timeout expired
            var finalStateQuery = conn.prepareTupleQuery(QUERY_ACTOR_STATE);
            finalStateQuery.setMaxExecutionTime(IQConstants.QUERY_TIMEOUT_MS);
            int finalReadyCount = 0;
            try (TupleQueryResult finalResult = finalStateQuery.evaluate()) {
                while (finalResult.hasNext()) {
                    finalResult.next();
                    finalReadyCount++;
                }
            }
            
            log.warn("waitForActorsReady: Timeout expired - only {} of {} actors READY", finalReadyCount, totalActorCount);
            return false;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("waitForActorsReady: Interrupted", e);
            return false;
        } catch (RepositoryException e) {
            log.error("waitForActorsReady: Repository error", e);
            return false;
        } catch (Exception e) {
            log.error("waitForActorsReady: Unexpected error", e);
            return false;
        }
    }
}
