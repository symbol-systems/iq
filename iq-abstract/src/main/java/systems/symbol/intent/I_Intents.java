package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

/**
 * Interface for managing multiple intents in IQ.
 *
 * This interface extends the I_Intent interface, adding functionality for managing multiple intents
 * IQ. It represents a collection of intents that can be executed concurrently
 * or sequentially to achieve complex behaviors.
 *
 * The primary purpose of this interface is to provide methods for adding and retrieving intents
 * within the execution context. The add() method allows adding another intent to the collection,
 * while the getIntents() method retrieves the set of intents currently present in the context.
 *
 * Intents managed by implementations of this interface are expected to be executed based on
 * the provided actor and state, similar to single intents. Each intent added to the collection
 * contributes to the overall behavior of the system.
 *
 * Implementations of this interface should maintain the order of intents as they are added
 * to the collection and ensure that intents are executed according to the desired execution
 * strategy (e.g., concurrently, sequentially).
 *
 * @see org.eclipse.rdf4j.model.IRI
 * @see systems.symbol.intent.I_Intent
 */
public interface I_Intents extends I_Intent {

/**
 * Adds another intent to the execution context.
 *
 * @param intent The intent to add to the execution context.
 * @return The IRI representing the added intent.
 */
IRI add(I_Intent intent);

/**
 * Retrieves the set of intents currently present in the execution context.
 *
 * @return The set of IRIs representing the intents.
 */
Set<IRI> getIntents();
}
