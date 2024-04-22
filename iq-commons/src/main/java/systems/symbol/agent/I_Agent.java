package systems.symbol.agent;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.platform.I_Self;
import systems.symbol.platform.I_StartStop;

/**
 * Interface for state-full agents.
 * An agent is capable of making decisions and executing tasks using pluggable state machines.
 */
public interface I_Agent extends I_Self, I_StartStop {

/**
 * Retrieves the working memory of the agent.
 *
 * @return The RDF4J model of working memory.
 */
Model getMemo();


/**
 * Retrieves the state machine associated with the agent.
 *
 * @return The I_StateMachine representing our decision tree.
 */
I_StateMachine<Resource> getStateMachine();

}
