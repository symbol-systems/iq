package systems.symbol.fsm;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A simple state machine implementation where transitions are defined explicitly by adding transitions
 * from one state to another.
 *
 * @param <T> The type representing states in the state machine.
 */
public class SimpleStateMachine<T> extends AbstractStateMachine<T> {

private final Map<T, Collection<T>> states = new HashMap<>(); // Map to store transitions between states

@Override
public void initialize() {
// Initialization logic if needed
}

/**
 * Adds a transition from one state to another.
 *
 * @param from The source state.
 * @param to   The target state.
 */
public void add(T from, T to) {
Collection<T> transitions = states.computeIfAbsent(from, k -> new HashSet<>());
transitions.add(to);
}

@Override
public boolean isAllowed(T targetState) {
return getTransitions(getState()).contains(targetState);
}

@Override
protected Collection<T> getTransitions(T state) {
return states.get(state);
}
}
