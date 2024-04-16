package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.I_Agent;
import systems.symbol.fleet.ExecutiveException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;

import java.util.concurrent.Future;

public interface I_Delegate<T> {
    Future<I_Decision<T>> delegate(I_Agent agent) throws StateException;
}
