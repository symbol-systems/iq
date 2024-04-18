package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.I_Intents;
import systems.symbol.intent.JSR233;
import systems.symbol.model.I_Self;
import systems.symbol.rdf4j.store.IQ_NS;
import systems.symbol.secrets.I_Secrets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a fleet of agents capable of executing intents within a symbolic system.
 */
public class AgenticFleet implements I_Fleet, I_Self {
protected final Logger log = LoggerFactory.getLogger(getClass());
Map<IRI, I_Agent> agents = new HashMap<>();
IRI self;
Model fleet;
I_Intents intents;
I_Secrets secrets;

/**
 * Constructs an AgenticFleet.
 *
 * @param selfthe self IRI representing this fleet
 * @param fleet   the RDF model representing the fleet
 * @param secrets the secrets manager for accessing agent secrets
 * @throws StateException if there is an issue with the state machine
 */
public AgenticFleet(IRI self, Model fleet, I_Secrets secrets) throws StateException {
this.self = self;
this.fleet = fleet;
this.secrets = secrets;
this.intents = new Executive(self, fleet);
this.intents.add( new JSR233(self, fleet, secrets) );
}

/**
 * Deploys agents from the fleet model and initializes them.
 *
 * @throws StateException if there is an issue with the state machine
 */
public void deploy() throws StateException {
Iterable<Statement> found = fleet.getStatements(null, IQ_NS.hasInitialState, null);
for (Statement s : found) {
IRI self = (IRI) s.getSubject();
I_Agent agent = newAgent(self);
agents.put(self, agent);
log.info("fleet.agent: {}", self);
}
log.info("fleet.deployed: {}", agents.keySet());
}

/**
 * Creates a new agent instance.
 *
 * @param selfthe self IRI representing the agent
 * @return the newly created agent
 * @throws StateException if there is an issue with the state machine
 */
public I_Agent newAgent(IRI self) throws StateException {
return new ExecutiveAgent(self, fleet, intents);
}

/**
 * Retrieves the agent with the given IRI.
 *
 * @param agent the IRI of the agent to retrieve
 * @return the agent corresponding to the given IRI
 */
@Override
public I_Agent getAgent(IRI agent) {
return agents.get(agent);
}

/**
 * Retrieves all agents in the fleet.
 *
 * @return a collection of all agents in the fleet
 */
public Collection<I_Agent> getAgents() {
return agents.values();
}

/**
 * Starts all agents in the fleet.
 *
 * @throws Exception if there is an issue starting the agents
 */
@Override
public void start() throws Exception {
if (agents.isEmpty()) deploy();
Set<IRI> iris = agents.keySet();
for (IRI agent : iris) {
start(agent);
}
}

/**
 * Stops all agents in the fleet.
 *
 * @throws Exception if there is an issue stopping the agents
 */
@Override
public void stop() throws Exception {
for (IRI agent : agents.keySet()) {
stop(agent);
}
}

/**
 * Starts the agent with the given IRI.
 *
 * @param self the IRI of the agent to start
 * @return the started agent
 * @throws Exception if there is an issue starting the agent
 */
@Override
public I_Agent start(IRI self) throws Exception {
I_Agent agent = getAgent(self);
if (agent == null) return null;
agent.start();
return agent;
}

/**
 * Stops the agent with the given IRI.
 *
 * @param self the IRI of the agent to stop
 * @return the state of the agent after stopping
 * @throws Exception if there is an issue stopping the agent
 */
@Override
public Resource stop(IRI self) throws Exception {
I_Agent agent = getAgent(self);
if (agent == null) return null;
agent.stop();
return agent.getStateMachine().getState();
}

/**
 * Retrieves the self IRI representing this fleet.
 *
 * @return the self IRI representing this fleet
 */
@Override
public IRI getSelf() {
return self;
}
}
