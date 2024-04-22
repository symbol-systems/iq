package systems.symbol.decide;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.tools.APIException;
import systems.symbol.finder.I_Finder;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.I_Prompt;
import systems.symbol.llm.I_Thread;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A simple decision-maker that uses random selection to pick next state.
 */
public class DelegateFinder extends SimpleDelegate<Resource> {
    I_Finder finder;
    private double minScore = 0.9;
    private int maxResults = 5;

    /**
     * Constructs a StochasticDecision with the provided state machine.
     *
     * @param fsm The state machine to make decisions based on.
     */
    public DelegateFinder(I_StateMachine<Resource> fsm, I_Finder finder) {
        super(fsm);
        this.finder = finder;
    }

    /**
     * Constructs a StochasticDecision with the provided state machine.
     *
     * @param fsm The state machine to make decisions based on.
     */
    public DelegateFinder(I_StateMachine<Resource> fsm, I_Finder finder, int maxResults, double minScore) {
        super(fsm);
        this.finder = finder;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    /**
     * Makes a decision by randomly picking a transition from the available transitions in the state machine.
     *
     * @return The selected transition resource.
     * @throws StateException If an error occurs while accessing the state machine.
     */
    @Override
    public Resource decide() throws StateException {
        Collection<Resource> transitions = getStateMachine().getTransitions();
        if (transitions.isEmpty()) return null;
        int decision = new Random().nextInt(transitions.size());
        return transitions.stream().skip(decision).findFirst().orElse(null);
    }

}
