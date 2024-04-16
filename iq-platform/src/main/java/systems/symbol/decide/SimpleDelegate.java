package systems.symbol.decide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;

public class SimpleDelegate<T> implements I_Delegate<T> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final I_StateMachine<T> fsm;

    private T decision;
    /**
     * Constructs a SimpleDecider with the provided state machine.
     *
     * @param fsm The state machine to make decisions based on.
     */
    public SimpleDelegate(I_StateMachine<T> fsm) {
        this.fsm = fsm;
    }

    public SimpleDelegate(I_StateMachine<T> fsm, T state) {
        this.fsm = fsm;
        choice(state);
    }

    public I_StateMachine<T> getStateMachine() {
        return this.fsm;
    }

    public I_Delegate<T> choice(T chosen) {
        decision = chosen == null?decision:chosen;
        return ()->decision;
    }

    /**
     * Makes a decision by randomly selecting a transition from the available transitions in the state machine.
     *
     * @return The selected transition resource.
     * @throws StateException If an error occurs while accessing the state machine.
     */
    @Override
    public T decide() throws StateException {
        if (decision==null) throw new StateException("undecided", null);
        return decision;
    }
}
