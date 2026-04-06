package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing actor/agent lifecycle and state transitions.
 * 
 * Handles:
 * - Actor discovery and registration
 * - State transitions (READY, THINKING, PAUSED, STOPPED)
 * - Checkpoint saving for recovery
 * - Health monitoring
 * 
 * State Machine:
 *   INITIALIZED → READY ⇄ THINKING → STOPPED
 * ↑
 *  PAUSED (optional)
 * 
 * @author Symbol Systems
 * @version 0.94+
 */
public class AgentService {
private static final Logger log = LoggerFactory.getLogger(AgentService.class);

/**
 * Actor state enumeration.
 */
public enum ActorState {
INITIALIZED("initialized", "Actor created, not yet ready"),
READY("ready", "Actor ready to accept requests"),
THINKING("thinking", "Actor processing request"),
PAUSED("paused", "Actor temporarily paused"),
STOPPED("stopped", "Actor shut down gracefully");

public final String label;
public final String description;

ActorState(String label, String description) {
this.label = label;
this.description = description;
}
}

/**
 * Actor state record with metadata.
 */
public static class ActorStatus {
public IRI actorIRI;
public ActorState state;
public long stateChangeTime;
public String lastError;
public long requestCount;
public long successCount;
public long failureCount;

public ActorStatus(IRI actorIRI, ActorState initialState) {
this.actorIRI = actorIRI;
this.state = initialState;
this.stateChangeTime = System.currentTimeMillis();
this.requestCount = 0;
this.successCount = 0;
this.failureCount = 0;
}
}

private final Map<String, ActorStatus> actorStateMap = new ConcurrentHashMap<>();

/**
 * Initialize agent service.
 * 
 * The service manages in-memory actor state transitions.
 * Persistence to RDF is handled by the CLI commands.
 */
public AgentService() {
log.info("[AgentService] initialized");
}

/**
 * Discover all actors/agents with known state.
 * 
 * Returns only actors whose state has been tracked by this service.
 * For initial discovery of all actors in realm, use RDF queries at the command level.
 * 
 * @return map of actor IRI strings to ActorStatus
 */
public Map<String, ActorStatus> getTrackedActors() {
return new HashMap<>(actorStateMap);
}

/**
 * Get current status of a specific actor.
 * 
 * @param actorIRI actor IRI
 * @return ActorStatus or null if not found
 */
public ActorStatus getStatus(IRI actorIRI) {
String key = actorIRI.stringValue();
ActorStatus status = actorStateMap.get(key);

if (status == null) {
// Create new status if not tracked
status = new ActorStatus(actorIRI, ActorState.INITIALIZED);
actorStateMap.put(key, status);
}

return status;
}

/**
 * Transition actor to READY state.
 * 
 * Validates prerequisites and updates state locally.
 * RDF persistence should be handled by the caller.
 * 
 * @param actorIRI actor to transition
 * @return success if transition allowed
 */
public boolean startActor(IRI actorIRI) {
ActorStatus status = getStatus(actorIRI);

// Validate state transition
if (status.state != ActorState.INITIALIZED && status.state != ActorState.PAUSED) {
log.warn("[AgentService] cannot start actor {} in state {}", actorIRI, status.state);
return false;
}

try {
// Perform initialization checks (could verify dependencies, secrets, etc.)
log.info("[AgentService] starting actor: {}", actorIRI);

// Update state locally
updateActorState(actorIRI, ActorState.READY);

log.info("[AgentService] actor ready: {}", actorIRI);
return true;
} catch (Exception e) {
log.error("[AgentService] error starting actor: {}", actorIRI, e);
status.lastError = e.getMessage();
return false;
}
}

/**
 * Gracefully stop an actor.
 * 
 * Saves checkpoint and transitions state locally.
 * RDF persistence should be handled by the caller.
 * 
 * @param actorIRI actor to stop
 * @return success if stop completed
 */
public boolean stopActor(IRI actorIRI) {
ActorStatus status = getStatus(actorIRI);

try {
log.info("[AgentService] stopping actor: {}", actorIRI);

// Save checkpoint before shutdown
saveCheckpoint(actorIRI, status);

// Update state locally
updateActorState(actorIRI, ActorState.STOPPED);

log.info("[AgentService] actor stopped: {}", actorIRI);
return true;
} catch (Exception e) {
log.error("[AgentService] error stopping actor: {}", actorIRI, e);
status.lastError = e.getMessage();
return false;
}
}

/**
 * Mark actor as thinking (processing request).
 * 
 * @param actorIRI actor to mark
 */
public void markThinking(IRI actorIRI) {
ActorStatus status = getStatus(actorIRI);
status.state = ActorState.THINKING;
status.stateChangeTime = System.currentTimeMillis();
status.requestCount++;
log.debug("[AgentService] actor is thinking: {}", actorIRI);
}

/**
 * Mark actor as ready (finished processing).
 * 
 * @param actorIRI actor to mark
 * @param success whether request succeeded
 */
public void markReady(IRI actorIRI, boolean success) {
ActorStatus status = getStatus(actorIRI);
status.state = ActorState.READY;
status.stateChangeTime = System.currentTimeMillis();

if (success) {
status.successCount++;
} else {
status.failureCount++;
}

log.debug("[AgentService] actor ready: {}", actorIRI);
}

/**
 * Temporarily pause actor (e.g., during maintenance).
 * 
 * @param actorIRI actor to pause
 */
public void pauseActor(IRI actorIRI) {
ActorStatus status = getStatus(actorIRI);
updateActorState(actorIRI, ActorState.PAUSED);
log.info("[AgentService] actor paused: {}", actorIRI);
}

/**
 * Update actor state in local tracking (persistence to RDF handled elsewhere).
 * 
 * @param actorIRI actor IRI
 * @param newState new state
 */
private void updateActorState(IRI actorIRI, ActorState newState) {
String key = actorIRI.stringValue();
ActorStatus status = actorStateMap.get(key);
if (status != null) {
status.state = newState;
status.stateChangeTime = System.currentTimeMillis();
log.debug("[AgentService] updated actor state: {} → {}", actorIRI, newState.label);
}
}

/**
 * Save actor checkpoint (actual persistence should be handled by RDF/storage layers).
 * 
 * @param actorIRI actor to checkpoint
 * @param status actor status
 */
private void saveCheckpoint(IRI actorIRI, ActorStatus status) {
log.debug("[AgentService] checkpoint ready for persistence: {}", actorIRI);
// Actual RDF persistence happens in the caller (CLI command)
}

/**
 * Get human-readable description of actor state.
 * 
 * @param state actor state
 * @return description string
 */
public String getStateDescription(ActorState state) {
return state.description;
}
}
