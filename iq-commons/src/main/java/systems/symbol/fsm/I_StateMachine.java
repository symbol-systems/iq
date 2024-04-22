package systems.symbol.fsm;

import org.eclipse.rdf4j.model.Resource;

import java.util.Collection;

/**
 * The {@code I_StateMachine} interface defines a generic contract for a finite state machine (FSM).
 * FSMs represent a system with a finite number of states and transitions between these states.
 *
 * @param <T> The type representing states in the FSM.
 */
public interface I_StateMachine<T> {

void initialize();
/**
 * Sets the initial state of the FSM.
 *
 * @param initialState The initial state to set for the FSM.
 * @return The modified FSM instance.
 */
I_StateMachine<T> setInitial(T initialState);

boolean isInitial();

/**
 * Retrieves the current state of the FSM.
 *
 * @return The current state of the FSM.
 */
T getState();

/**
 * Retrieves the possible transitions from the current state of the FSM.
 *
 * @return A collection of possible transitions from the current state.
 */
Collection<T> getTransitions();

/**
 * Registers a listener to be notified when a state transition occurs.
 *
 * @param listener The listener to be notified on state transitions.
 */
//void listen(I_StateListener<T> listener);

void listen(I_StateListener<T> listener);

/**
 * Attempts to transition to a new state based on specified guards and conditions.
 *
 * @param targetState The target state to transition to.
 * @return The modified FSM instance after the transition.
 * @throws StateException If the transition is not allowed or encounters an error.
 */
T transition(T targetState) throws StateException;

/**
 * Checks if a transition to the specified target state is allowed based on guards and conditions.
 *
 * @param targetState The target state to check for permission.
 * @return {@code true} if the transition is allowed, {@code false} otherwise.
 */
boolean isAllowed(T targetState);

public void add(T from, T to);
}
