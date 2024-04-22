package systems.symbol.fsm;

/**
 * Interface for listening to state transitions in a finite state machine.
 *
 * @param <T> The type of the states in the finite state machine.
 */
public interface I_StateListener<T> {
/**
 * Called when a state transition occurs in the finite state machine.
 *
 * @param from The state transitioning from.
 * @param to   The state transitioning to.
 * @return True if the transition is allowed, false if the transition should be vetoed.
 */
boolean onTransition(T from, T to) throws StateException;
}
