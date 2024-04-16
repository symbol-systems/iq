package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.decide.I_Decision;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fleet.ExecutiveException;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.I_Intent;
import systems.symbol.secrets.I_Secrets;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ExecutiveAgent extends IntentAgent implements I_Decision<Resource> {
    I_Secrets secrets;
    I_Delegate<Resource> manager;

    /**
     * The ExecutiveAgent handles stateful Intentions.
     * @param model The RDF4J model to be associated with the agent.
     * @param self The identity of the agent
     */

    public ExecutiveAgent(@NotNull Model model, IRI self) throws StateException {
        this(model, self, null, null, null);
    }

    public ExecutiveAgent(@NotNull Model model, IRI self, I_Secrets secrets, I_Intent intent) throws StateException {
        this(model, self, secrets, intent, null);
    }

    public ExecutiveAgent(@NotNull Model model, IRI self, I_Secrets secrets, I_Intent intent, I_Delegate<Resource> manager) throws StateException {
        super(self, model, new Executive(self, model, intent), new SimpleBindings());
        this.secrets = secrets;
        this.manager = manager;
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
            Set<IRI> executed = execute(getSelf(), to, bindings);
            log.info("onTransition: {} -> {} --> {}", to, executed, bindings);
        try {
            Resource next = decide();
            if (next!=null) getStateMachine().transition(next);
        } catch (StateException e) {
            throw new RuntimeException(e);
        }
        return true;
    }


    @Override
    public Resource decide() throws StateException {
        Collection<Resource> choices = getStateMachine().getTransitions();
        if (choices.isEmpty()) return null;
        if (choices.size()==1) return choices.iterator().next();
        if (manager == null) return null;
        Future<I_Decision<Resource>> delegated = manager.delegate(this);
        try {
            return delegated.get().decide();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
