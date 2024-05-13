package systems.symbol.decide;

import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class AgentManager implements I_Decide<Resource> {
    Map<I_Agent, AgentDecision> decisions = new HashMap<>();

    public AgentManager() {
    }

    public AgentManager(I_Agent agent) {
        delegate(agent);
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
        AgentDecision decision = new AgentDecision(agent);
        this.decisions.put(agent, decision);
        return decision;
    }

    public Collection<Resource> getIntents(I_Agent agent) {
        AgentDecision decision = decisions.get(agent);
        if (decision == null) return null;
        return decision.agent.getStateMachine().getTransitions();
    }

    public void decide(I_Agent agent, Resource intent) throws StateException {
        AgentDecision decision = decisions.get(agent);
        if (decision == null) throw new StateException("unknown", agent);
        boolean found = decision.agent.getStateMachine().getTransitions().contains(intent);
        if (!found) throw new StateException(agent.getSelf().stringValue(), intent.stringValue());
        decision.complete(() -> intent);
    }
}
