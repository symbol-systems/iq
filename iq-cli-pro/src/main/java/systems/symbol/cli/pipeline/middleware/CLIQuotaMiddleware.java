package systems.symbol.cli.pipeline.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.cli.pipeline.CLICallContext;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CLI Quota Middleware.
 */
public class CLIQuotaMiddleware implements I_Middleware<CLICallContext> {
private static final Logger log = LoggerFactory.getLogger(CLIQuotaMiddleware.class);

private static final Map<String, Integer> LIMITS = Map.of(
"trigger", 10,
"boot", 1,
"run", 5
);

private static final Map<String, QuotaCounter> counters = new ConcurrentHashMap<>();

@Override
public int getOrder() {
return 300;
}

@Override
public void process(CLICallContext ctx, I_Chain<CLICallContext> chain) throws KernelException {
String commandName = ctx.commandName();
String principal = ctx.principal();

Integer limit = LIMITS.get(commandName);
if (limit == null) {
log.debug("Quota: no limit for command '{}'", commandName);
chain.proceed(ctx);
return;
}

String key = principal + ":" + commandName;
QuotaCounter counter = counters.computeIfAbsent(key, k -> new QuotaCounter(limit));

if (!counter.tryConsume()) {
String msg = "Quota exceeded for " + commandName + ": limit " + limit + " per minute";
log.warn("QUOTA VIOLATION: principal={}, command={}, limit={}", principal, commandName, limit);
throw new KernelException("cli.quota.exceeded", msg);
}

log.debug("Quota: principal={}, command={}, count={}/{}", 
  principal, commandName, counter.getCount(), limit);

chain.proceed(ctx);
}

@Override
public String displayName() {
return "CLIQuotaMiddleware";
}

private static class QuotaCounter {
private final int limit;
private final AtomicInteger count = new AtomicInteger(0);
private final AtomicLong resetTimeMs = new AtomicLong(System.currentTimeMillis() + 60_000);

QuotaCounter(int limit) {
this.limit = limit;
}

boolean tryConsume() {
long now = System.currentTimeMillis();
if (now >= resetTimeMs.get()) {
count.set(0);
resetTimeMs.set(now + 60_000);
}

int current = count.getAndIncrement();
return current < limit;
}

int getCount() {
return count.get();
}
}
}
