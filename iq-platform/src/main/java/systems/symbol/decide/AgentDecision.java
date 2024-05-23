package systems.symbol.decide;

import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AgentDecision extends CompletableFuture<I_Delegate<Resource>> {
protected final Logger log = LoggerFactory.getLogger(getClass());
I_Delegate<Resource> decision;
I_Agent agent;
public AgentDecision(I_Agent agent) {
this.agent = agent;
}
public AgentDecision(I_Agent agent, Resource decision) throws StateException {
this.agent = agent;
decide(decision);
}

public void decide(Resource decision) throws StateException {
log.info("agent.decide: {} @ {} & {}", decision, agent, agent.getStateMachine().getState());
this.decision = () -> decision;
this.agent.getStateMachine().transition(decision);
}
@Override
public boolean isDone() {
return decision!=null;
}

@Override
public I_Delegate<Resource> get() throws InterruptedException, ExecutionException {
return decision;
}
}
