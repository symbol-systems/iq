package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

/**
 * Interface for an intent.
 * An intent is a contextual request to execute an action that may have side effects.
 */
public interface I_Intents extends I_Intent {

    /**
     * Executes the intent based on the provided actor and state.
     *
     * @param intent Add another intent to the execution context.
     */
    IRI add(I_Intent intent);
    Set<IRI> getIntents();
}
