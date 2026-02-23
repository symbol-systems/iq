package systems.symbol.fsm;

import java.util.Collection;

/**
 * Interface for defining a finite state machine (FSM) in IQ.
 *
 * FSMs represent systems with a finite number of states and transitions between
 * these states. This interface
 * defines the contract that FSM implementations must adhere to IQ.
 *
 * The primary purpose of this interface is to provide methods for initializing
 * the FSM, setting and retrieving
 * the initial state, getting the current state, retrieving possible
 * transitions, registering state transition
 * listeners, performing state transitions, and checking the validity of
 * transitions.
 *
 * The generic type parameter T represents the type of states in the FSM.
 *
 * Implementations of this interface should encapsulate the behavior and
 * transitions of the FSM, ensuring
 * that it operates correctly within the context of the IQ environment.
 *
 */
public interface I_StateMachine<T> {

    /**
     * Initializes the state machine.
     * 
     * @throws StateException
     */
    void initialize() throws StateException;

    /**
     * Sets the initial state of the FSM.
     *
     * @param initialState The initial state to set for the FSM.
     * @return The modified FSM instance.
     */
    I_StateMachine<T> setInitial(T initialState);

    /**
     * Sets the initial state of the FSM.
     *
     * @param currentState Force current state to set for the FSM - does not trigger
     *                     transition.
     * @return The modified FSM instance.
     */
    void setCurrentState(T currentState);

    /**
     * Checks if the current state is the initial state.
     *
     * @return true if the current state is the initial state, otherwise false.
     */
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
    void listen(I_StateListener<T> listener);

    /**
     * Attempts to transition to a new state based on specified guards and
     * conditions.
     *
     * @param targetState The target state to transition to.
     * @return The modified FSM instance after the transition.
     * @throws StateException If the transition is not allowed or encounters an
     *                        error.
     */
    T transition(T targetState) throws StateException;

    /**
     * Checks if a transition to the specified target state is allowed based on
     * guards and conditions.
     *
     * @param targetState The target state to check for permission.
     * @return {@code true} if the transition is allowed, {@code false} otherwise.
     */
    boolean isAllowed(T targetState);

    /**
     * Adds a transition from one state to another.
     *
     * @param from The source state of the transition.
     * @param to   The target state of the transition.
     */
    void add(T from, T to);
}
