package systems.symbol.intent;

import systems.symbol.fsm.StateException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import javax.script.Bindings;
import java.util.Set;

/**
 * Interface for an intent in IQ.
 *
 * An intent represents a contextual request to execute an action that may have side effects.
 * This interface defines the contract that intent implementations must adhere to within
 * the IQ ecosystem.
 *
 * The primary purpose of this interface is to provide a method for executing the intent
 * based on the provided actor, state, and contextual bindings. The execute() method takes
 * an actor representing the entity initiating the action, a state representing the current
 * execution step, and bindings providing contextual information for the action.
 *
 * The execute() method returns a set of IRIs representing the results of the intent execution.
 * These IRIs may represent resources, identifiers, or other relevant entities resulting from
 * the action execution.
 *
 * Intent implementations are expected to handle any potential errors or exceptions that may
 * occur during the execution process. The execute() method throws a StateException to signal
 * such errors, providing a standardized mechanism for error handling and reporting.
 *
 * Implementations of this interface should be designed to be stateless and thread-safe,
 * allowing them to be used in various contexts within the IQ environment.
 *
 * @see org.eclipse.rdf4j.model.IRI
 * @see org.eclipse.rdf4j.model.Resource
 * @see javax.script.Bindings
 * @see systems.symbol.fsm.StateException
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
