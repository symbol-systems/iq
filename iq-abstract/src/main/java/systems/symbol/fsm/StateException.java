/**
 * Exception class representing errors related to the state machine in the IQ operating environment.
 *
 * This exception is thrown to indicate errors or exceptional conditions encountered during the
 * execution of state machine operations. It provides additional information about the state
 * where the error occurred.
 */

package systems.symbol.fsm;

public class StateException extends Exception {
    // The state where the error occurred
    public Object state;

    /**
     * Constructs a new StateException with the specified detail message and state.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     * @param state   the state where the error occurred.
     */
    public StateException(String message, Object state) {
        super(message);
        this.state = state;
    }

    /**
     * Constructs a new StateException with the specified detail message, state, and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     * @param state   the state where the error occurred.
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     */
    public StateException(String message, Object state, Throwable cause) {
        super(message, cause);
        this.state = state;
    }
}
