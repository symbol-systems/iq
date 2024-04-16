package systems.symbol.intent;

import systems.symbol.fsm.StateException;
import systems.symbol.model.I_Self;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import javax.script.Bindings;
import java.util.Set;

/**
 * Interface for an intent within a symbolic system.
 * An intent is a contextual request to execute an action that may have side effects.
 */
public interface I_Intent {

    /**
     * Executes the intent based on the provided actor and state.
     *
     * @param actor The actor of the execution.
     * @param state  The state representing the current execution step.
     * @param bindings  The 'my' bindings contextual the action.
     * @return A set of IRIs resulting from the execution.
     * @throws StateException If an error occurs during execution.
     */
    Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException;
}
