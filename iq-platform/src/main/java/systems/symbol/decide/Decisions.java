package systems.symbol.decide;

import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Decisions implements I_Decide<Resource> {
    Map<I_Agent, HumanDecision<Resource>> decisions = new HashMap<>();

    public Decisions() {
    }

    public Decisions(I_Agent agent) {
        delegate(agent);
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
        HumanDecision<Resource> decision = new HumanDecision<>(agent);
        this.decisions.put(agent, decision);
        return decision;
    }

    public Collection<Resource> getIntents(I_Agent agent) {
        HumanDecision<Resource> decision = decisions.get(agent);
        if (decision == null) return null;
        return decision.agent.getStateMachine().getTransitions();
    }

    public void decide(I_Agent agent, Resource intent) throws StateException {
        HumanDecision<Resource> decision = decisions.get(agent);
        if (decision == null) return;
        boolean found = decision.agent.getStateMachine().getTransitions().contains(intent);
        if (!found) throw new StateException(agent.getSelf().stringValue(), intent.stringValue());
        decision.complete(() -> intent);
    }
}
