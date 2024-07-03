/**
 * Interface for deferring responsibility for decision-making in IQ.
 *
 * Decision delegation is a key concept in the IQ framework, allowing components to defer responsibility
 * for making decisions to specialized decision-making entities. This interface defines the contract
 * that such decision-making delegates must adhere to IQ.
 *
 * The primary purpose of this interface is to delegate decision-making based on the current state or context.
 * It provides a method for making decisions and returning the result, encapsulated as a generic type.
 * Decision-making delegates may utilize various strategies, algorithms, or external resources to
 * determine the appropriate course of action.
 *
 * This interface is generic, allowing decision-making delegates to return decisions of any desired type.
 * Implementations should specify the concrete type of decision returned by the decide() method.
 *
 * Decision-making delegates are expected to handle any potential errors or exceptions that may occur
 * during the decision-making process. The decide() method throws a StateException to signal such errors,
 * providing a standardized mechanism for error handling and reporting.
 *
 * Implementations of this interface should be designed to be stateless and thread-safe, allowing them
 * to be used in various contexts within the IQ environment.
 *
 * @param <T> the type of decision returned by the decide() method
 * @throws StateException if an error occurs during the decision-making process
 *
 * @see systems.symbol.fsm.StateException
 */
package systems.symbol.decide;

import systems.symbol.fsm.StateException;

/**
 * Defer responsibility for taking decisions.
 */
public interface I_Delegate<T> {

    /**
     * Retrieve a decision intent.
     *
     * @return the decision as a resource
     * @throws StateException if an error occurs during the decision-making process
     */
    T intent() throws StateException;

}
