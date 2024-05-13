package systems.symbol.decide;
/**
 * Interface for defining guards in IQ.
 *
 * Guards are mechanisms used to determine whether a specific action or intent is allowed
 * based on certain conditions or criteria. This interface defines the contract that guard
 * implementations must adhere to IQ.
 *
 * The primary purpose of this interface is to evaluate whether a given action, represented
 * by an actor and an intent, is permitted according to the rules defined by the guard.
 * Guards may inspect the state of the environment, the properties of the actor, the nature
 * of the intent, or any other relevant factors to make this determination.
 *
 * This interface is generic, allowing guards to work with objects of any desired type.
 * The allows() method takes two parameters: the actor representing the entity attempting
 * to perform the action, and the intent representing the action or behavior in question.
 * Implementations should specify the concrete types of the actor and intent.
 *
 * Guards are expected to return a boolean value indicating whether the action is allowed
 * or not. If the guard allows the action, it returns true; otherwise, it returns false.
 *
 * Implementations of this interface should be designed to be stateless and thread-safe,
 * allowing them to be used in various contexts within the IQ environment.
 *
 * @param <T> the type of objects representing the actor and intent
 */

public interface I_Guard<T> {
    /**
     * Determines whether the specified actor is allowed to perform the given intent.
     *
     * @param actor the entity attempting to perform the action
     * @param intent the action or behavior in question
     * @return true if the action is allowed, false otherwise
     */

    boolean allows(T actor, T intent);
}
