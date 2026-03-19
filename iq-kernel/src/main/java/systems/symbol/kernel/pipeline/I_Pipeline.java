package systems.symbol.kernel.pipeline;

import systems.symbol.kernel.KernelException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordered chain-of-responsibility pipeline.
 *
 * <p>implementations sort middleware by {@link I_Middleware#getOrder()} at build
 * time and return a single {@link I_Chain} that threads through the full list.
 *
 * <p>Surface modules implement this interface with their own context type:
 * <pre>{@code
 * // in iq-mcp
 * public class MCPConnectPipeline implements I_Pipeline<MCPCallContext> { … }
 * }</pre>
 *
 * <p>A default implementation is provided via {@link #of(List)}.
 *
 * @param <CTX> the call context type
 */
public interface I_Pipeline<CTX extends KernelCallContext> {

/**
 * Build a single continuation from the ordered middleware list.
 *
 * @param middleware the middleware set (will be sorted by order)
 * @return the head of the chain; calling {@code proceed(ctx)} runs all middleware
 */
I_Chain<CTX> build(List<I_Middleware<CTX>> middleware);

/**
 * Convenience factory. Sorts middleware by {@link I_Middleware#getOrder()} and
 * returns the head {@link I_Chain} directly.
 *
 * @param <C>the context type
 * @param middleware the middleware set to chain
 * @return the head of the composed chain
 */
static <C extends KernelCallContext> I_Chain<C> of(List<I_Middleware<C>> middleware) {
List<I_Middleware<C>> ordered = new ArrayList<>(middleware);
ordered.sort(Comparator.comparingInt(I_Middleware::getOrder));
return buildChain(ordered, 0, ctx -> {});
}

/** Recursive chain construction. */
private static <C extends KernelCallContext> I_Chain<C> buildChain(
List<I_Middleware<C>> list, int index, I_Chain<C> tail) {
if (index >= list.size()) return tail;
I_Middleware<C> current = list.get(index);
I_Chain<C> next = buildChain(list, index + 1, tail);
return ctx -> current.process(ctx, next);
}
}
