package systems.symbol.kernel;

/**
 * Root unchecked exception for the IQ kernel.
 *
 * <p>All kernel subpackages (pipeline, command, event, workspace) throw subtypes
 * of this class. Surface adapters (REST, MCP, CLI, Camel) catch specific subtypes
 * and translate to their own error formats (HTTP status, MCP error object, exit
 * code, or Camel exchange failure) at the boundary.
 *
 * <p>Hierarchy:
 * <pre>
 * KernelException
 *   ├── KernelBootException      — startup / realm init failure
 *   ├── KernelAuthException      — authn / authz failure
 *   ├── KernelCommandException   — command execution failure
 *   ├── KernelSecretException    — secret resolution failure
 *   └── KernelBudgetException    — budget / quota exceeded
 * </pre>
 */
public class KernelException extends RuntimeException {

    private final String code;

    public KernelException(String code, String message) {
        super(message);
        this.code = code;
    }

    public KernelException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Machine-readable error code, e.g. {@code "kernel.boot.realm-init"}.
     * Surface adapters use this to produce structured error responses.
     */
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + code + "]: " + getMessage();
    }
}
