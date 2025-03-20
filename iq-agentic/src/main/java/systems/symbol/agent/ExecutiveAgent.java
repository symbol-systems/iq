package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import systems.symbol.platform.IQ_NS;
import javax.script.Bindings;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class ExecutiveAgent extends IntentAgent implements I_Delegate<Resource> {

    /**
     * The ExecutiveAgent makes simple decisions and delegates other to manager.
     * 
     * @param self     The identity of the agent
     * @param thoughts The working memory of the agent as an RDF4J Model.
     */
    public ExecutiveAgent(@NotNull IRI self, @NotNull Model thoughts, I_Intent intent, Bindings bindings)
            throws StateException {
        super(self, thoughts, thoughts, intent, bindings);
    }

    public ExecutiveAgent(@NotNull IRI self, @NotNull Model ground, @NotNull Model thoughts, I_Intent intent,
            Bindings bindings) throws StateException {
        super(self, ground, thoughts, intent, bindings);
    }

    /**
     * Handles transitions .
     *
     * @param from The resource representing the source state oSf the transition.;
     * @param to   The resource representing the target state of the transition.
     * @return true if the transition is handled successfully, false otherwise.
     */
    @Override
    public boolean onTransition(Resource from, Resource to) throws StateException {
        if (seen.contains(to)) {
            log.info("agent.seen: {} @ {} ==> {}", self, from, to);
            getStateMachine().setInitial(to);
            return true;
        }
        seen.add(to);
        if (to.isIRI() && IQ_NS.TO.equals(to)) {
            Resource transitioned = getStateMachine().transition(getSelf());
            log.info("agent.self: {} -> {}", getSelf(), transitioned);
            return transitioned != null & Objects.equals(transitioned, getSelf());
        }
        // try {
        log.info("agent.execute: {} @ {}", self, getStateMachine().getState());
        Set<IRI> executed = execute(getSelf(), to, bindings);
        Resource next = intent();
        log.info("agent.decided: {} --> {} <-- {}", from, next, executed);
        if (next == null) {
            log.info("agent.undecided: {} @ {}", getSelf(), from);
            return false; // don't veto, we may try again
        }
        if (getStateMachine().getState().equals(next)) {
            log.info("agent.same: {} =? {}", next, from);
            return !next.equals(from);
        }
        Resource transitioned = getStateMachine().transition(next);
        log.info("agent.transitioned: {} --> {}", from, transitioned);
        seen.add(transitioned);
        return next.equals(transitioned);
    }

    /**
     * Determines the appropriate next-step based on the current transitions
     * (choices) of the state machine.
     * If a single choice exists, the decision is simply made.
     * If multiple choices are available, we delegate the decision-making process to
     * the manager, if one exists.
     *
     * @return the selected resource
     * @throws StateException if there is an issue with the state machine
     */
    @Override
    public Resource intent() throws StateException {
        Collection<Resource> choices = getStateMachine().getTransitions();
        log.info("agent.deciding: {} @ {} -> {}", getSelf(), getStateMachine().getState(), choices);
        if (choices.isEmpty())
            return null;
        if (choices.size() == 1)
            return choices.iterator().next();
        return getStateMachine().getState();
    }

    public void start() throws Exception {
        this.seen.clear();
        super.start();
    }
}
