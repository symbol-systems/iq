package systems.symbol.decide;

import systems.symbol.agent.I_Agent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HumanDecision<T> extends CompletableFuture<I_Delegate<T>> {
I_Delegate<T> decision;
I_Agent agent;
public HumanDecision(I_Agent agent) {
this.agent = agent;
}
public HumanDecision(I_Agent agent, T decision) {
this.agent = agent;
this.decision = () -> decision;
}

@Override
public boolean isDone() {
return decision!=null;
}

@Override
public I_Delegate<T> get() throws InterruptedException, ExecutionException {
return decision;
}
}
