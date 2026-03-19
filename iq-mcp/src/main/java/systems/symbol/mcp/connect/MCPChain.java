package systems.symbol.mcp.connect;

import systems.symbol.kernel.KernelAuthException;
import systems.symbol.kernel.KernelBudgetException;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

/**
 * MCPChain — result-bearing chain-of-responsibility continuation for the MCP surface.
 *
 * <p>Analogous to the kernel's {@link I_Chain}{@code <MCPCallContext>} but differs
 * in that {@link #proceed(MCPCallContext)} returns an {@link I_MCPResult} rather
 * than {@code void}. This return-type difference prevents formal extension of
 * {@link I_Chain}{@code <MCPCallContext>}; the static {@link #from(I_Chain)} adapter
 * bridges between the two contracts.
 *
 * <p>The implementation is constructed by {@link MCPConnectPipeline} and is
 * not meant to be created by application code.
 */
@FunctionalInterface
public interface MCPChain {

/**
 * Proceed to the next element in the pipeline.
 *
 * @param ctx the (possibly enriched) call context
 * @return non-null result
 * @throws MCPException on any downstream failure
 */
I_MCPResult proceed(MCPCallContext ctx) throws MCPException;

/**
 * Adapt a kernel {@link I_Chain}{@code <MCPCallContext>} to a result-bearing
 * {@link MCPChain}.
 *
 * <p>After the void kernel chain completes, the result is read from
 * {@code ctx.get("mcp.result")}. Any {@link KernelException} thrown by the
 * kernel chain is mapped to the corresponding {@link MCPException} code:
 * <ul>
 *   <li>{@link KernelAuthException} (UNAUTHENTICATED) → 401</li>
 *   <li>{@link KernelAuthException} (UNAUTHORIZED) → 403</li>
 *   <li>{@link KernelBudgetException} → 429</li>
 *   <li>other {@link KernelException} → 500</li>
 * </ul>
 *
 * @param kernelChain the void kernel continuation to wrap
 * @return a result-bearing {@link MCPChain} backed by the kernel chain
 */
static MCPChain from(I_Chain<MCPCallContext> kernelChain) {
return ctx -> {
try {
kernelChain.proceed(ctx);
} catch (KernelAuthException ke) {
if (ke.getKind() == KernelAuthException.Kind.UNAUTHENTICATED) {
throw MCPException.unauthorized(ke.getMessage());
} else {
throw MCPException.forbidden(ke.getMessage());
}
} catch (KernelBudgetException ke) {
throw MCPException.quotaExceeded(ke.getMessage());
} catch (KernelException ke) {
throw new MCPException(500, ke.getMessage(), ke);
}
I_MCPResult stored = ctx.get("mcp.result");
return stored != null ? stored : MCPResult.ok("", "text/plain");
};
}
}
