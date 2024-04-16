package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.fsm.StateException;

import javax.script.Bindings;
import java.util.Set;

/**
 * Interface for an intent within a symbolic system.
 * An intent is a contextual request to execute an action that may have side effects.
 */
public interface I_Intents extends I_Intent{

/**
 * Executes the intent based on the provided actor and state.
 *
 * @param intent Add another intent to the execution context.
 */
IRI add(I_Intent intent);
}
