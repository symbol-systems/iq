package systems.symbol.kernel;

/**
 * Thrown when a kernel command fails during execution.
 * Replaces {@code StateException} and {@code APIException} for cross-surface use.
 */
public class KernelCommandException extends KernelException {

    public KernelCommandException(String message) {
        super("kernel.command", message);
    }

    public KernelCommandException(String code, String message) {
        super(code, message);
    }

    public KernelCommandException(String message, Throwable cause) {
        super("kernel.command", message, cause);
    }

    public KernelCommandException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
