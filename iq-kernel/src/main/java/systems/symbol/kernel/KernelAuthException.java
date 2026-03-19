package systems.symbol.kernel;

/**
 * Thrown when authentication or authorisation fails.
 * Replaces the UNAUTHORIZED / FORBIDDEN paths of {@code OopsException}.
 *
 * <p>Surface adapters translate this to:
 * <ul>
 *   <li>REST — {@code 401} or {@code 403} HTTP response</li>
 *   <li>MCP  — {@code MCPException.unauthorized()} / {@code .forbidden()}</li>
 *   <li>CLI  — exit code 77 (NOPERM) with message to stderr</li>
 *   <li>Camel — {@code AuthException} set on the exchange</li>
 * </ul>
 */
public class KernelAuthException extends KernelException {

    public enum Kind { UNAUTHENTICATED, UNAUTHORIZED }

    private final Kind kind;

    public KernelAuthException(Kind kind, String message) {
        super("kernel.auth." + kind.name().toLowerCase(), message);
        this.kind = kind;
    }

    public KernelAuthException(Kind kind, String message, Throwable cause) {
        super("kernel.auth." + kind.name().toLowerCase(), message, cause);
        this.kind = kind;
    }

    public static KernelAuthException unauthenticated(String message) {
        return new KernelAuthException(Kind.UNAUTHENTICATED, message);
    }

    public static KernelAuthException unauthorized(String message) {
        return new KernelAuthException(Kind.UNAUTHORIZED, message);
    }

    public Kind getKind() {
        return kind;
    }
}
