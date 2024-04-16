package systems.symbol.decide;

import systems.symbol.fsm.StateException;

/**
 * The <code>I_Decision</code> interface represents a decision-making process within a symbolic system.
 * Implementations of this interface are responsible for taking decisions based on the current state.
 */
public interface I_Decision<T> {

    /**
     * Makes a decision based on the provided state.
     *
     * @return the decision as a resource
     * @throws StateException if an error occurs during the decision-making process
     */
    T decide() throws StateException;

}
