package systems.symbol.decide;

import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;

import java.util.Collection;
import java.util.Random;

/**
 * A simple decision-maker that uses random selection to pick next state.
 */
public class StochasticDecision<T> extends SimpleDecision<T> {

    /**
     * Constructs a StochasticDecision with the provided state machine.
     *
     * @param fsm The state machine to make decisions based on.
     */
    public StochasticDecision(I_StateMachine<T> fsm) {
        super(fsm);
    }

    /**
     * Makes a decision by randomly picking a transition from the available transitions in the state machine.
     *
     * @return The selected transition resource.
     * @throws StateException If an error occurs while accessing the state machine.
     */
    @Override
    public T decide() throws StateException {
        Collection<T> transitions = getStateMachine().getTransitions();
        if (transitions.isEmpty()) return null;
        int decision = new Random().nextInt(transitions.size());
        return transitions.stream().skip(decision).findFirst().orElse(null);
    }
}
