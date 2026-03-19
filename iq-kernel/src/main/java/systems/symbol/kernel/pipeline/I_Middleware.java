package systems.symbol.kernel.pipeline;

import systems.symbol.kernel.KernelException;

/**
 * Single element in a chain-of-responsibility pipeline.
 *
 * <p>Typed on a context that extends {@link KernelCallContext}. This generic
 * type parameter keeps {@code iq-kernel} free of surface-specific types:
 * <ul>
 *   <li>{@code iq-mcp} declares {@code I_MCPPipeline extends I_Middleware<MCPCallContext>}</li>
 *   <li>Tests use {@code I_Middleware<KernelCallContext>} directly</li>
 * </ul>
 *
 * <p>Rules:
 * <ol>
 *   <li>Never mutate immutable fields of the context.</li>
 *   <li>Enrich the context via {@link KernelCallContext#set}.</li>
 *   <li>Short-circuit by NOT calling {@code chain.proceed(ctx)}.</li>
 *   <li>Implementations MUST be thread-safe (they are singletons).</li>
 *   <li>Declare execution order via {@link #getOrder()} — lower runs first.</li>
 * </ol>
 *
 * @param <CTX> the call context type
 */
public interface I_Middleware<CTX extends KernelCallContext> {

/**
 * Execution order — lower numbers run first.
 * The pipeline sorts middleware by this value at boot time.
 */
int getOrder();

/**
 * Process the request.
 *
 * @param ctx   the mutable call envelope
 * @param chain the continuation — call {@code chain.proceed(ctx)} to continue
 * @throws KernelException on any middleware-level failure
 */
void process(CTX ctx, I_Chain<CTX> chain) throws KernelException;

/** Display name for logging and diagnostics. */
default String displayName() { return getClass().getSimpleName(); }
}
