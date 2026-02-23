package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intents;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Bootstrap;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;

import javax.script.SimpleBindings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a fleet of agents capable of executing intents.
 */
public class AgenticFleet implements I_Fleet, I_Self, I_Bootstrap {
protected final Logger log = LoggerFactory.getLogger(getClass());
Map<IRI, I_Agent> agents = new HashMap<>();
IRI self;
Model fleet, thoughts;
I_Intents intents;
I_Secrets secrets;

/**
 * Constructs an AgenticFleet.
 *
 * @param selfthe self IRI representing this fleet
 * @param fleet   the RDF model representing the fleet
 * @param secrets the secrets manager for accessing agent secrets
 */
public AgenticFleet(IRI self, Model fleet, Model thoughts, I_Intents intents, I_Secrets secrets)
throws StateException {
boot(self, fleet);
this.secrets = secrets;
this.intents = intents;
this.thoughts = thoughts;
}

public void boot(IRI self, Model model) {
this.self = self;
this.fleet = model;
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
I_Agent agent = deploy(self);
agents.put(self, agent);
log.info("fleet.agent: {}", self);
}
log.info("fleet.deployed: {}", agents.keySet());
}

/**
 * Creates a new agent instance.
 *
 * @param self the self IRI representing the agent
 * @return the newly created agent
 * @throws StateException if there is an issue with the state machine
 */
/**
 * Deploy a new actor instance.
 *
 * @param actor the Agent IRI
 * @return the newly created actor
 * @throws StateException if there is an issue with the deployment
 */
public I_Agent deploy(IRI actor) throws StateException {
if (this.agents.containsKey(actor))
return agents.get(actor);
ExecutiveAgent agent = new ExecutiveAgent(actor, fleet, intents, new SimpleBindings());
agents.put(actor, agent);
return agent;
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
if (agents.isEmpty())
deploy();
Set<IRI> iris = agents.keySet();
log.info("fleet.start: {}", iris);
for (IRI agent : iris) {
start(agent);
}
log.info("fleet.started: {}", iris);
}

/**
 * Stops all agents in the fleet.
 *
 * @throws Exception if there is an issue stopping the agents
 */
@Override
public void stop() {
for (IRI agent : agents.keySet()) {
try {
stop(agent);
} catch (Exception e) {

}
}
}

/**
 * Starts the agent with the given IRI.
 *
 * @param self the IRI of the agent to start
 * @throws Exception if there is an issue starting the agent
 */
@Override
public void start(IRI self) throws Exception {
I_Agent agent = getAgent(self);
if (agent == null)
return;
agent.start();
}

/**
 * Stops the agent with the given IRI.
 *
 * @param self the IRI of the agent to stop
 * @throws Exception if there is an issue stopping the agent
 */
@Override
public void stop(IRI self) throws Exception {
I_Agent agent = getAgent(self);
if (agent == null)
return;
agent.stop();
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
