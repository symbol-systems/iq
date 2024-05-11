package systems.symbol.fsm;

import systems.symbol.decide.I_Guard;

import java.util.Collection;

public class GuardedStateMachine<T> implements I_StateMachine<T> {
I_StateMachine<T> fsm;
I_Guard<T> ask;
T actor;
public GuardedStateMachine(T actor, I_StateMachine<T> fsm, I_Guard<T> ask) {
this.actor = actor;
this.fsm = fsm;
this.ask = ask;
// assume fsm is initialized()
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
return ask.allows(actor, intent);
}

@Override
public void add(T from, T to) {
fsm.add(from,to);
}
}
