package systems.symbol.decide;

import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;

import java.util.concurrent.Future;

public interface I_Decide<T> {
    Future<I_Delegate<T>> delegate(I_Agent agent) throws StateException;
}
