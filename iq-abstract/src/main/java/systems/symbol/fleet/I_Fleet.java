package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.I_Agent;
import systems.symbol.platform.I_StartStop;

import java.util.Collection;

/**
 * Interface for managing a fleet of agents in IQ.
 *
 * This interface defines the contract for managing a fleet of agents, where each agent represents
 * an autonomous entity capable of decision-making and task execution IQ.
 *
 * The primary purpose of this interface is to provide methods for retrieving individual agents
 * from the fleet, as well as starting and stopping agents as needed. The getAgent() method allows
 * retrieving a specific agent based on its unique identifier (IRI), while the getAgents() method
 * returns a collection of all agents in the fleet.
 *
 * Agents managed by implementations of this interface are expected to implement the I_Agent interface
 * and adhere to the lifecycle management provided by the I_StartStop interface. This includes methods
 * for starting and stopping agents, which are inherited from the I_StartStop interface.
 *
 * The start() and stop() methods provide functionality for starting and stopping individual agents
 * within the fleet. These methods throw exceptions if any errors occur during the start or stop process,
 * allowing for error handling and recovery.
 *
 * Implementations of this interface should manage the lifecycle of agents effectively, ensuring that
 * agents are started and stopped appropriately to maintain system integrity and performance.
 *
 * @see org.eclipse.rdf4j.model.IRI
 * @see systems.symbol.agent.I_Agent
 * @see systems.symbol.platform.I_StartStop
 */
public interface I_Fleet extends I_StartStop {
    /**
     * Retrieves the agent with the specified identifier from the fleet.
     *
     * @param agent The unique identifier of the agent.
     * @return The agent with the specified identifier.
     */
    I_Agent getAgent(IRI agent);

    /**
     * Retrieves a collection containing all agents in the fleet.
     *
     * @return A collection of agents in the fleet.
     */
    Collection<I_Agent> getAgents();

    /**
     * Starts the agent with the specified identifier.
     *
     * @param agent The unique identifier of the agent to start.
     * @throws Exception if an error occurs during the start process.
     */
    void start(IRI agent) throws Exception;

    /**
     * Stops the agent with the specified identifier.
     *
     * @param agent The unique identifier of the agent to stop.
     * @throws Exception if an error occurs during the stop process.
     */
    void stop(IRI agent) throws Exception;
}
