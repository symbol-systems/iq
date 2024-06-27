package systems.symbol.agent;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.platform.I_Self;
import systems.symbol.platform.I_StartStop;

/**
 * Interface for stateful agents in the IQ operating system.
 * * 
 * An agent in this context refers to an autonomous entity capable of making decisions
 * and executing tasks based on its internal state and external inputs. This interface
 * defines the core functionality that all stateful agents must implement within the 
 * IQ framework.
 * * 
 * The primary capabilities provided by this interface include:
 * - Access to the agent's working memory: Agents have access to a working memory,
 *   represented as an RDF4J model, which stores information and knowledge necessary 
 *   for decision-making and task execution.
 * - Access to the agent's state machine: Agents utilize pluggable state machines to
 *   manage their decision-making process. The state machine represents a decision 
 *   tree or finite state machine (FSM) that guides the agent through various states 
 *   and actions based on the current context and goals.
 * - Lifecycle management: Agents can be started, stopped, and managed through this
 *   interface, providing control over their execution and behavior within the 
 *   operating system.
 *
 * This interface is designed to be implemented by concrete classes representing 
 * specific types of agents IQ. By adhering 
 * to this interface, developers can create custom agents that seamlessly integrate 
 * with the IQ operating system and leverage its capabilities for symbolic cognition.
 *
 * @see systems.symbol.fsm.I_StateMachine
 * @see systems.symbol.platform.I_Self
 * @see systems.symbol.platform.I_StartStop
 */
public interface I_Agent extends I_Self, I_StartStop {

/**
 * Retrieves the working memory of the agent.
 *
 * @return The RDF4J model representing the working memory.
 */
Model getThoughts();


/**
 * Retrieves the state machine associated with the agent.
 *
 * @return The state machine representing the decision tree.
 */
I_StateMachine<Resource> getStateMachine();

}
