package systems.symbol.mcp.connect;

import systems.symbol.kernel.KernelAuthException;
import systems.symbol.kernel.KernelBudgetException;
import systems.symbol.kernel.KernelCommandException;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import org.eclipse.rdf4j.model.IRI;

/**
 * I_MCPPipeline — pipeline element SPI for the MCP surface.
 *
 * <p>Extends {@link I_Middleware}{@code <MCPCallContext>} so that MCP middleware
 * instances conform to the kernel's generic chain-of-responsibility contract and
 * can be composed via {@link systems.symbol.kernel.pipeline.I_Pipeline}.
 *
 * <p>The surface-specific {@link #process(MCPCallContext, MCPChain)} overload is
 * result-bearing (returns {@link I_MCPResult}). A {@code default} bridge provides
 * the kernel's {@code void process(MCPCallContext, I_Chain)} contract automatically,
 * so concrete implementations only need to implement the MCP overload.
 *
 * <p><b>Design rules</b>
 * <ol>
 *   <li>Never mutate {@link MCPCallContext#rawInput()} — it is immutable.</li>
 *   <li>Enrichment goes via {@link MCPCallContext#set(String, Object)}.</li>
 *   <li>Short-circuit by returning a non-null result directly (do not call chain).</li>
 *   <li>Always invoke {@code chain.proceed(ctx)} unless short-circuiting.</li>
 *   <li>Implementations must be <em>thread-safe</em> (they are singletons).</li>
 * </ol>
 *
 * <p><b>Versioning</b> — for async/reactive support a future
 * {@code I_MCPReactiveMiddleware} will add a
 * {@code Publisher<I_MCPResult> process(MCPCallContext, MCPChain)} overload
 * while keeping this synchronous contract stable.
 */
public interface I_MCPPipeline extends I_Middleware<MCPCallContext> {

/**
 * Self IRI of this middleware instance.
 * Must match the IRI declared in the {@code mcp:pipeline} named graph.
 * (MCP-surface specific — not part of {@link I_Middleware}.)
 */
IRI getSelf();

// getOrder() and displayName() are inherited from I_Middleware<MCPCallContext>.

/**
 * MCP-surface result-bearing process overload.
 *
 * <p>Coexists with the kernel's {@code void process(MCPCallContext, I_Chain)}
 * because the third parameter type differs ({@link MCPChain} vs
 * {@link I_Chain}{@code <MCPCallContext>}), making these two distinct overloads.
 *
 * @param ctx   the mutable call envelope
 * @param chain the result-bearing MCP continuation
 * @return non-null result (from downstream or short-circuit)
 * @throws MCPException on any middleware-level failure
 */
I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException;

/**
 * Bridge from the kernel's void {@link I_Middleware} contract to the
 * result-bearing MCP overload.
 *
 * <p>Implemented as a {@code default} so concrete middleware classes need only
 * implement {@link #process(MCPCallContext, MCPChain)}. The bridge:
 * <ol>
 *   <li>Wraps the kernel {@link I_Chain} via {@link MCPChain#from(I_Chain)} into
 *   a result-bearing {@link MCPChain} that reads {@code "mcp.result"} from
 *   the context after the void chain returns.</li>
 *   <li>Calls the result-bearing {@link #process(MCPCallContext, MCPChain)}.</li>
 *   <li>Stores the returned {@link I_MCPResult} at {@code "mcp.result"} so
 *   downstream kernel-contract callers can retrieve it.</li>
 *   <li>Re-throws any {@link MCPException} as the appropriate
 *   {@link KernelException} subtype.</li>
 * </ol>
 */
@Override
default void process(MCPCallContext ctx, I_Chain<MCPCallContext> chain)
throws KernelException {
try {
I_MCPResult result = process(ctx, MCPChain.from(chain));
if (result != null) {
ctx.set("mcp.result", result);
}
} catch (MCPException e) {
switch (e.getCode()) {
case 401 -> throw KernelAuthException.unauthenticated(e.getMessage());
case 403 -> throw KernelAuthException.unauthorized(e.getMessage());
case 429 -> throw new KernelBudgetException(e.getMessage());
default  -> throw new KernelCommandException(
"mcp." + e.getCode(), e.getMessage(), e);
}
}
}
}
