package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QuotaGuardMiddleware — per-principal, per-tool rate limiting (order: 40).
 *
 * <p>Uses in-memory sliding window (1-minute tumbling window).
 * For multi-instance deployments, use RDF4J or Redis-backed counters.
 *
 * <p>Limits configured via {@code mcp-server-config.ttl}:
 * {@code mcp:quotaLimit} (default: 200/min) and {@code mcp:writeToolLimit} (default: 10/min).
 */
public class QuotaGuardMiddleware implements I_MCPPipeline {

private static final IRI SELF = Values.iri("urn:mcp:pipeline/QuotaGuard");
private static final int DEFAULT_LIMIT = 200;
private static final int WRITE_TOOL_LIMIT = 10;
private static final long WINDOW_MS = 60_000L;

private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
private final int quotaLimit;
private final int writeToolLimit;

public QuotaGuardMiddleware() {
this.quotaLimit = DEFAULT_LIMIT;
this.writeToolLimit = WRITE_TOOL_LIMIT;
}

public QuotaGuardMiddleware(int quotaLimit, int writeToolLimit) {
this.quotaLimit = quotaLimit;
this.writeToolLimit = writeToolLimit;
}

@Override public IRI getSelf()  { return SELF; }
@Override public int getOrder() { return 40; }

@Override
public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
String key = ctx.principal() + ":" + ctx.toolName();
int limit = resolveLimit(ctx.toolName());
WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());

int used = counter.increment(limit);
if (used < 0) {
throw MCPException.quotaExceeded("rate limit exceeded");
}
ctx.set(MCPCallContext.KEY_QUOTA_USED, used);
return chain.proceed(ctx);
}

private int resolveLimit(String toolName) {
if (toolName.contains("update") || toolName.startsWith("actor.")) {
return writeToolLimit;
}
return quotaLimit;
}

private static final class WindowCounter {
private long windowStart = Instant.now().toEpochMilli();
private final AtomicInteger count = new AtomicInteger(0);

synchronized int increment(int limit) {
long now = Instant.now().toEpochMilli();
if (now - windowStart > WINDOW_MS) {
windowStart = now;
count.set(0);
}
int current = count.incrementAndGet();
return current > limit ? -1 : current;
}
}
}
