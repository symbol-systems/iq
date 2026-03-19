package systems.symbol.kernel.pipeline;

/**
 * Single-method access control check.
 *
 * <p>This interface separates the <em>authorization decision</em> from the
 * Camel / HTTP / MCP lifecycle hook that enforces it. Surface adapters
 * implement the hook and delegate to an {@code I_AccessPolicy}:
 *
 * <pre>{@code
 * // in iq-camel — IQRoutePolicy.onExchangeBegin
 * public void onExchangeBegin(Route route, Exchange exchange) {
 *     KernelCallContext ctx = contextFromExchange(exchange);
 *     if (!accessPolicy.allows(ctx)) {
 *         exchange.setException(new AuthException("access denied"));
 *     }
 * }
 * }</pre>
 *
 * <p>Implementations should be stateless and thread-safe. An implementation
 * backed by SPARQL policy triples can live in {@code iq-platform}; a
 * permissive (allow-all) version is provided here for testing.
 */
@FunctionalInterface
public interface I_AccessPolicy {

    /**
     * Returns {@code true} if the request described by {@code ctx} is permitted.
     *
     * @param ctx the current call context (principal and realm already set by auth middleware)
     * @return {@code true} to allow, {@code false} to deny
     */
    boolean allows(KernelCallContext ctx);

    /** Permissive policy — always allows. Use in tests and open-access deployments. */
    static I_AccessPolicy allowAll() {
        return ctx -> true;
    }

    /** Strict policy — always denies. Use in tests that verify denial paths. */
    static I_AccessPolicy denyAll() {
        return ctx -> false;
    }
}
