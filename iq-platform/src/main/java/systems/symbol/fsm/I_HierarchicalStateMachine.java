package systems.symbol.fsm;

import java.util.Collection;

/**
 * The {@code I_HierarchicalStateMachine} interface extends the basic FSM functionality
 * to support hierarchical state machines, where states can have sub-states.
 *
 * @param <T> The type representing states in the FSM.
 */
public interface I_HierarchicalStateMachine<T> extends I_StateMachine<T> {

/**
 * Adds a sub-state to the specified parent state.
 *
 * @param parentState The parent state to which the sub-state will be added.
 * @param subStateThe sub-state to be added.
 * @return The modified hierarchical FSM instance.
 */
I_HierarchicalStateMachine<T> addSubState(T parentState, T subState);

/**
 * Retrieves the sub-states of the specified parent state.
 *
 * @param parentState The parent state for which sub-states are retrieved.
 * @return A collection of sub-states for the specified parent state.
 */
Collection<T> getSubStates(T parentState);

/**
 * Checks if the specified state has any sub-states.
 *
 * @param parentState The state to check for sub-states.
 * @return {@code true} if the state has sub-states, {@code false} otherwise.
 */
boolean hasSubStates(T parentState);

/**
 * Sets the initial sub-state for the specified parent state.
 *
 * @param parentState  The parent state for which the initial sub-state is set.
 * @param initialSubState The initial sub-state to set.
 * @return The modified hierarchical FSM instance.
 */
I_HierarchicalStateMachine<T> setInitialSubState(T parentState, T initialSubState);

/**
 * Retrieves the initial sub-state for the specified parent state.
 *
 * @param parentState The parent state for which the initial sub-state is retrieved.
 * @return The initial sub-state for the specified parent state.
 */
T getInitialSubState(T parentState);

/**
 * Resets the state of the hierarchical FSM to the specified parent state and its initial sub-state.
 *
 * @param parentState The parent state to which the FSM is reset.
 * @return The modified hierarchical FSM instance after the reset.
 */
I_HierarchicalStateMachine<T> resetToInitialState(T parentState);
}
