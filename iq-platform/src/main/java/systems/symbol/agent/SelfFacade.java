package systems.symbol.agent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;

public class SelfFacade {
    private final I_Agent agent;

    public SelfFacade(I_Agent agent) {
        this.agent = agent;
    }

    public Resource state() throws IllegalArgumentException, StateException {
        return this.agent.getStateMachine().getState();
    }

    public String as(String to) {
        if (to.indexOf(":") < 0)
            return agent.getSelf() + "#" + to;
        else
            return to;
    }

    public Future<I_Delegate<Resource>> decide(String to) throws IllegalArgumentException, StateException {
        Resource decide = Values.iri(as(to));
        return CompletableFuture.completedFuture(() -> decide);
    }

    public Resource to(Resource to) throws StateException {
        agent.getStateMachine().transition(to);
        return state();
    }

    public boolean can(String to) {
        return can(Values.iri(as(to)));
    }

    public boolean can(Resource to) {
        return agent.getStateMachine().isAllowed(to);
    }

    public Collection<String> can() {
        return agent.getStateMachine().getTransitions()
                .stream()
                .map(iri -> iri.stringValue())
                .collect(Collectors.toList());
    }

    public String toString() {
        return agent.getSelf().toString();
    }
}
