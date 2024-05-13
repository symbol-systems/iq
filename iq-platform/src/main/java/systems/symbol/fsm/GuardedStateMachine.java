package systems.symbol.fsm;

import systems.symbol.decide.I_Guard;

import java.util.Collection;

/**
 * We pass state transitions to the underlying state machine
 * only if the guard to determines the transition is allowed.
 *
 * @param <T> The type representing states in the state machine.
 */
public class GuardedStateMachine<T> implements I_StateMachine<T> {

private final I_StateMachine<T> fsm; // The underlying state machine
private final I_Guard<T> guard; // The guard condition
private final T actor; // The entity associated with the state machine

/**
 * Constructs a GuardedStateMachine with the provided actor, underlying state machine, and guard condition.
 *
 * @param actor The entity associated with the state machine.
 * @param fsm   The underlying state machine.
 * @param guard The guard condition determining whether a transition is allowed.
 */
public GuardedStateMachine(T actor, I_StateMachine<T> fsm, I_Guard<T> guard) {
this.actor = actor;
this.fsm = fsm;
this.guard = guard;
}

@Override
public void initialize() {
fsm.initialize();
}

@Override
public I_StateMachine<T> setInitial(T initialState) {
return fsm.setInitial(initialState);
}

@Override
public boolean isInitial() {
return fsm.isInitial();
}

@Override
public T getState() {
return fsm.getState();
}

@Override
public Collection<T> getTransitions() {
return fsm.getTransitions();
}

@Override
public void listen(I_StateListener<T> listener) {
fsm.listen(listener);
}

@Override
public T transition(T targetState) throws StateException {
return fsm.transition(targetState);
}

@Override
public boolean isAllowed(T intent) {
return guard.allows(actor, intent);
}

@Override
public void add(T from, T to) {
fsm.add(from, to);
}
}
