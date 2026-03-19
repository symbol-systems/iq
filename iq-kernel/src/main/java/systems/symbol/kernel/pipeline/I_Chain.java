package systems.symbol.kernel.pipeline;

import systems.symbol.kernel.KernelException;

/**
 * Chain-of-responsibility continuation.
 *
 * <p>Each {@link I_Middleware} receives an {@code I_Chain} that, when invoked,
 * runs the next middleware in the pipeline (or the terminal handler if all
 * middleware have been processed).
 *
 * <p>Surface pipelines (e.g. {@code MCPConnectPipeline} in {@code iq-mcp}) use
 * the typed variant {@code I_Chain<MCPCallContext>} where
 * {@code MCPCallContext extends KernelCallContext}.
 *
 * @param <CTX> the call context type — must extend {@link KernelCallContext}
 */
@FunctionalInterface
public interface I_Chain<CTX extends KernelCallContext> {

    /**
     * Proceed to the next element in the pipeline.
     *
     * @param ctx the (possibly enriched) call context
     * @throws KernelException on any downstream failure
     */
    void proceed(CTX ctx) throws KernelException;
}
