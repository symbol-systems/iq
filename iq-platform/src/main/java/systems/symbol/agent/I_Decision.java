package systems.symbol.agent;

import org.eclipse.rdf4j.model.Resource;
import systems.symbol.fsm.StateException;

/**
 * The <code>I_Decision</code> interface represents a decision-making process within a symbolic system.
 * Implementations of this interface are responsible for taking decisions based on the current state.
 */
public interface I_Decision {

/**
 * Makes a decision based on the provided state.
 *
 * @param state the current state of the system
 * @return the decision as a resource
 * @throws StateException if an error occurs during the decision-making process
 */
Resource decide(Resource state) throws StateException;

}
