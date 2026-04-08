package systems.symbol.rdf4j.fedx.materialization;

/**
 * Thrown when source materialization fails.
 *
 * <p>Covers failures in:
 * - Remote source fetch (network issues, endpoint errors)
 * - Data transformation (schema mismatch, type errors)
 * - Cache operations (storage full, corruption)
 */
public class MaterializationException extends Exception {

  /**
   * Construct with a message.
   *
   * @param message descriptive error message
   */
  public MaterializationException(String message) {
    super(message);
  }

  /**
   * Construct with a message and cause.
   *
   * @param message descriptive error message
   * @param cause the underlying exception
   */
  public MaterializationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Construct with a cause only.
   *
   * @param cause the underlying exception
   */
  public MaterializationException(Throwable cause) {
    super(cause);
  }
}
