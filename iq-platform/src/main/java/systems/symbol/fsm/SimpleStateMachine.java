package systems.symbol.fsm;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SimpleStateMachine<T> extends AbstractStateMachine<T> {
    Map<T, Collection<T>> states = new HashMap<>();

    @Override
    public void initialize() {
        // do
    }

    public void add(T from, T to) {
        Collection<T> ts = states.computeIfAbsent(from, k -> new HashSet<>());
        ts.add(to);
    }
    @Override
    public boolean isAllowed(T targetState) {
        return getTransitions(getState()).contains(targetState);
    }

    @Override
    protected Collection<T> getTransitions(T state) {
        return states.get(getState());
    }
}
