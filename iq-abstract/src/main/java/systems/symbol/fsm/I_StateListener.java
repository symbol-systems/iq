package systems.symbol.fsm;

/**
 * Interface for defining a listener for state transitions within a finite state machine (FSM)
 * in the IQ operating environment.
 *
 * State listeners are notified when a state transition occurs in the FSM, allowing external
 * components to react to state changes and potentially influence the transition process.
 *
 * The generic type parameter T represents the type of states in the FSM.
 *
 * Implementations of this interface should define the behavior to be executed when a state
 * transition occurs. The onTransition() method is called with the source state (from) and
 * the target state (to) of the transition. The method should return true if the transition
 * is allowed or false if the transition should be vetoed.
 *
 * If an error occurs during the transition process, implementations can throw a StateException
 * to signal the error condition.
 *
 */
public interface I_StateListener<T> {
    /**
     * Called when a state transition occurs in the finite state machine.
     *
     * @param from The state transitioning from.
     * @param to   The state transitioning to.
     * @return True if the transition is allowed, false if the transition should be vetoed.
     * @throws StateException if an error occurs during the state transition process.
     */
    boolean onTransition(T from, T to) throws StateException;
}
