package systems.symbol.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract implementation of the State Machine interface providing common
 * functionalities.
 *
 * @param <T> The type representing states in the state machine.
 */
public abstract class AbstractStateMachine<T> implements I_StateMachine<T> {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected T initialState;
protected T currentState;
private final Set<I_StateListener<T>> listeners = new HashSet<>();

/**
 * Sets the initial state of the state machine.
 *
 * @param initialState The initial state to set.
 * @return The state machine instance.
 */
@Override
public I_StateMachine<T> setInitial(T initialState) {
setCurrentState(this.initialState = initialState);
assert initialState != null && this.initialState == this.currentState;
return this;
}

/**
 * Retrieves the current state of the state machine.
 *
 * @return The current state.
 */
@Override
public T getState() {
return currentState;
}

/**
 * Adds a state listener to the state machine.
 *
 * @param listener The listener to add.
 */
@Override
public void listen(I_StateListener<T> listener) {
listeners.add(listener);
}

/**
 * Performs a transition to the target state in the state machine.
 *
 * @param targetState The state to transition to.
 * @return The state after transition.
 * @throws StateException If the transition is not allowed.
 */
@Override
public T transition(T targetState) throws StateException {
if (targetState == null)
return getState();
boolean allowed = isAllowed(targetState);
if (allowed) {
T fromState = currentState;
setCurrentState(targetState);
boolean ok = notifyListeners(fromState, targetState);
log.info("transition?: {} @ {} => {}", ok, fromState, targetState);
if (!ok) {
log.warn("veto: {} -> {} @ {}", fromState, targetState, getState());
setCurrentState(fromState);
}
return getState();
}
throw new StateException("from: " + getState() + " to: " + targetState + " denied.", targetState);
}

/**
 * Sets the current state of the state machine.
 *
 * @param targetState The state to set as current.
 */
public void setCurrentState(T targetState) {
currentState = targetState;
}

/**
 * Checks if transitioning to the target state is allowed.
 *
 * @param targetState The state to transition to.
 * @return True if transition is allowed, false otherwise.
 */
@Override
public abstract boolean isAllowed(T targetState);

public boolean isInitial() {
return initialState.equals(getState());
}

/**
 * Retrieves all possible transitions from the given state.
 *
 * @return Collection of possible transitions.
 */
@Override
public Collection<T> getTransitions() {
return getTransitions(getState());
}

/**
 * Retrieves all possible transitions from the given state.
 *
 * @param state The state to get transitions from.
 * @return Collection of possible transitions.
 */
protected abstract Collection<T> getTransitions(T state);

/**
 * Notifies all listeners about a transition and allows them to veto it.
 *
 * @param from The state transitioned from.
 * @param to   The state transitioned to.
 * @return True if no listener vetoes the transition, false otherwise.
 */
protected boolean notifyListeners(T from, T to) throws StateException {
boolean ok = true;
for (I_StateListener<T> listener : listeners) {
boolean _ok = listener.onTransition(from, to);
log.info("state.notify: {} -> {} -> {} @ {}", _ok, from, to, getState());
ok = ok & _ok;
}
return ok;
}
}
