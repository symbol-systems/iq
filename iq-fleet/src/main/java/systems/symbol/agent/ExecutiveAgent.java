package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.decide.I_Delegate;
import systems.symbol.decide.I_Decide;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.I_Intent;
import systems.symbol.secrets.I_Secrets;

import javax.script.SimpleBindings;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ExecutiveAgent extends IntentAgent implements I_Delegate<Resource> {
    I_Secrets secrets;
    I_Decide<Resource> manager;
    Set<Resource> seen = new HashSet<>();

    /**
     * The ExecutiveAgent makes and delegates stateful decisions.
     * @param self The identity of the agent
     * @param memo The working memory of the agent as an RDF4J Model.
     */

    public ExecutiveAgent(@NotNull Model memo, IRI self, I_Secrets secrets, I_Intent intent) throws StateException {
        this(memo, self, secrets, new Executive(self, memo, intent), null);
    }

    public ExecutiveAgent(@NotNull Model memo, IRI self, I_Secrets secrets, I_Intent intent, I_Decide<Resource> manager) throws StateException {
        super(self, memo, new Executive(self, memo, intent), new SimpleBindings());
        this.secrets = secrets;
        this.manager = manager;
    }

    public void resume() {
        this.seen.clear();
    }

    public Executive getExecutive() {
        return (Executive) this.intent;
    }

    /**
     * Handles transitions within the symbolic system.
     *
     * @param from The resource representing the source state of the transition.
     * @param to   The resource representing the target state of the transition.
     * @return true if the transition is handled successfully, false otherwise.
     */
    @Override
    public boolean onTransition(Resource from, Resource to) throws StateException {
        log.info("onTransition: {} -> {} --> {}", self, to, bindings);
        Set<IRI> executed = execute(getSelf(), to, bindings);
        Resource next = decide();
        log.info("onDecision: {} -> {} -> {}", executed, next, seen);
        if (next!=null && !seen.contains(next)) {
            getStateMachine().transition(next);
            seen.add(next);
        }
        return true;
    }

    /**
     * Determines the appropriate next-step based on the current transitions (choices) of the state machine.
     * If a single choice exists, the decision is simply made.
     * If multiple choices are available, we delegate the decision-making process to the manager.
     *
     * @return the selected resource
     * @throws StateException if there is an issue with the state machine
     */
    @Override
    public Resource decide() throws StateException {
        Collection<Resource> choices = getStateMachine().getTransitions();
        log.info("onDecision: {} -> {}", choices, manager==null?"solo":manager);
        if (choices.isEmpty()) return null;
        if (choices.size()==1) return choices.iterator().next();
        if (manager == null) return null;
        Future<I_Delegate<Resource>> delegated = manager.delegate(this);
        try {
            return delegated.get().decide();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
