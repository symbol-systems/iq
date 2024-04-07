package systems.symbol.agent;

import systems.symbol.fsm.I_StateMachine;
import systems.symbol.model.HasIdentity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

/**
 * Interface defining the contract for agents with pluggable skills.
 * An agent is capable of making decisions and executing tasks using pluggable state machines.
 */
public interface I_Agent extends HasIdentity, I_Decision {

    /**
     * Sets the RDF4J model for the agent.
     *
     * @param model The RDF4J model to be set.
     */
    void setModel(Model model);

    /**
     * Retrieves the RDF4J model associated with the agent.
     *
     * @return The RDF4J model.
     */
    Model getModel();


    /**
     * Retrieves the state machine associated with a given task.
     *
     * @return The state machine associated with the task.
     */
    I_StateMachine<Resource> getStateMachine();

    /**
     * Learn skills by associating tasks with a corresponding state machine.
     *
     * @param fsm The state machine associated with the task for learning.
     */
    void learn(I_StateMachine<Resource> fsm);
}
