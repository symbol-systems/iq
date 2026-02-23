package systems.symbol.decide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SimpleDelegate<T> implements I_Decide<T> {
protected final Logger log = LoggerFactory.getLogger(getClass());
I_Delegate<T> decider;
/**
 * Constructs a simple delegate.
 *
 * @param decider A delegated decision-maker.
 */
public SimpleDelegate(I_Delegate<T> decider) {
this.decider = decider;
}

@Override
public Future<I_Delegate<T>> delegate(I_Agent agent) throws StateException {
CompletableFuture<I_Delegate<T>> future = new CompletableFuture<>();
future.complete(()->decider.intent());
return future;
}
}
