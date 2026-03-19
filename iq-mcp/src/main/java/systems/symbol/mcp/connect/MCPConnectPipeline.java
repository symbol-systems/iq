package systems.symbol.mcp.connect;

import systems.symbol.kernel.KernelAuthException;
import systems.symbol.kernel.KernelBudgetException;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;
import systems.symbol.kernel.pipeline.I_Pipeline;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MCPConnectPipeline — the ordered execution pipeline.
 *
 * <p>Implements {@link I_Pipeline}{@code <MCPCallContext>} so the kernel can
 * compose and inspect this pipeline via the generic contract. The
 * {@link #build(List)} method exposes the pre/post middleware chain as a
 * kernel {@link I_Chain}; the full adapter-wrapping execution path is available
 * via {@link #execute(MCPCallContext, MCPChain)}.
 *
 * <p>This class wires together the registered {@link I_MCPPipeline} elements
 * and the terminal handler (a {@link MCPChain} supplied by the adapter layer).
 * It implements the chain-of-responsibility pattern in declaration order.
 *
 * <p><b>Default pipeline order</b> (configurable via {@code mcp:pipeline} named graph):
 * <ol>
 *   <li>AuthGuard (10)</li>
 *   <li>TrustZoneGuard (20)</li>
 *   <li>ACLFilter (30)</li>
 *   <li>QuotaGuard (40)</li>
 *   <li>BudgetGuard (50)</li>
 *   <li>CacheInterceptor (60)</li>
 *   <li>InputTransformer (70)</li>
 *   <li>→ adapter.doExecute()</li>
 *   <li>OutputTransformer (80)</li>
 *   <li>AuditWriter (90)</li>
 *   <li>MetricsEmitter (100)</li>
 * </ol>
 *
 * <p>The split between pre-adapter and post-adapter middleware is handled
 * automatically: middleware with {@code order < 80} run before the adapter,
 * those with {@code order >= 80} run after (wrapping).
 */
public class MCPConnectPipeline implements I_Pipeline<MCPCallContext> {

private static final Logger log = LoggerFactory.getLogger(MCPConnectPipeline.class);

private final List<I_MCPPipeline> preMiddleware;
private final List<I_MCPPipeline> postMiddleware;

public MCPConnectPipeline(List<I_MCPPipeline> all) {
List<I_MCPPipeline> sorted = new ArrayList<>(all);
sorted.sort(Comparator.comparingInt(I_MCPPipeline::getOrder));
this.preMiddleware  = sorted.stream().filter(m -> m.getOrder() < 80).toList();
this.postMiddleware = sorted.stream().filter(m -> m.getOrder() >= 80).toList();
}

/**
 * Execute the complete pipeline around {@code adapterChain}.
 *
 * @param ctx  the request context
 * @param adapterChain the adapter's terminal handler
 * @return the final result (may come from a middleware short-circuit)
 */
public I_MCPResult execute(MCPCallContext ctx, MCPChain adapterChain) {
Instant start = ctx.startTime();
try {
// Build the pre-adapter chain
MCPChain chain = buildChain(preMiddleware, 0,
// The "middle" step: run adapter, then wrap into post chain
innerCtx -> {
I_MCPResult raw = adapterChain.proceed(innerCtx);
return runPostMiddleware(raw, innerCtx);
});
return chain.proceed(ctx);
} catch (MCPException ex) {
log.warn("[MCP] pipeline error [{}] code={} msg={}", ctx.traceId(), ex.getCode(), ex.getMessage());
return ex.toResult();
} catch (KernelAuthException ke) {
int httpCode = (ke.getKind() == KernelAuthException.Kind.UNAUTHENTICATED) ? 401 : 403;
log.warn("[MCP] kernel auth error [{}] code={} msg={}", ctx.traceId(), httpCode, ke.getMessage());
return MCPResult.error(httpCode, ke.getMessage());
} catch (KernelBudgetException ke) {
log.warn("[MCP] kernel budget exceeded [{}] msg={}", ctx.traceId(), ke.getMessage());
return MCPResult.error(429, ke.getMessage());
} catch (KernelException ke) {
log.warn("[MCP] kernel error [{}] code={} msg={}", ctx.traceId(), ke.getCode(), ke.getMessage());
return MCPResult.error(500, ke.getMessage());
} catch (Exception ex) {
log.error("[MCP] unexpected error [{}]", ctx.traceId(), ex);
return MCPResult.error(500, "Internal error: " + ex.getMessage());
} finally {
long ms = Duration.between(start, Instant.now()).toMillis();
log.debug("[MCP] {} completed in {}ms [{}]", ctx.toolName(), ms, ctx.traceId());
}
}

/* ── I_Pipeline<MCPCallContext> ───────────────────────────────────────── */

/**
 * Implements the kernel {@link I_Pipeline} contract by composing all
 * registered middleware (pre + post, sorted by order) into a single
 * {@link I_Chain}{@code <MCPCallContext>} via {@link I_Pipeline#of(List)}.
 *
 * <p>The terminal handler of the returned chain is a no-op; the
 * full adapter-wrapping execution (with a real terminal handler) is
 * available via {@link #execute(MCPCallContext, MCPChain)}.
 */
@Override
public I_Chain<MCPCallContext> build(List<I_Middleware<MCPCallContext>> middleware) {
return I_Pipeline.of(middleware);
}

/* ── private helpers ─────────────────────────────────────────────────── */

private MCPChain buildChain(List<I_MCPPipeline> list, int index, MCPChain tail) {
if (index >= list.size()) return tail;
I_MCPPipeline current = list.get(index);
MCPChain next = buildChain(list, index + 1, tail);
return ctx -> current.process(ctx, next);
}

private I_MCPResult runPostMiddleware(I_MCPResult result, MCPCallContext ctx) throws MCPException {
// Post-middleware handlers receive the result via context attribute
ctx.set("mcp.result", result);
MCPChain chain = buildChain(postMiddleware, 0, ignored -> ctx.get("mcp.result"));
return chain.proceed(ctx);
}
}
