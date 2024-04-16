package systems.symbol.decide;

import systems.symbol.fsm.StateException;

/**
 * Defer responsibility for taking decisions.
 */
public interface I_Delegate<T> {

/**
 * Makes a decision based on the provided state.
 *
 * @return the decision as a resource
 * @throws StateException if an error occurs during the decision-making process
 */
T decide() throws StateException;

}
