package systems.symbol.policy.decorator;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:rate-limit");

private final int limit;
private final Duration window;
private final ConcurrentMap<String, Entry> history = new ConcurrentHashMap<>();

public RateLimitEnforcer(int limit, Duration window) {
if (limit <= 0) {
throw new IllegalArgumentException("limit must be positive");
}
this.limit = limit;
this.window = Objects.requireNonNull(window, "window is required");
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
if (input.principal() == null) {
return PolicyResult.deny(PolicyVocab.REASON_ANONYMOUS_PRINCIPAL, ENFORCER);
}

var now = Instant.now();
var key = input.principal().toString();
Entry entry = history.compute(key, (k, existing) -> {
if (existing == null || now.isAfter(existing.windowReset)) {
return new Entry(AtomicInteger::new, now.plus(window));
}
existing.count.incrementAndGet();
return existing;
});

if (entry.count.get() > limit) {
return PolicyResult.deny(PolicyVocab.REASON_RATE_LIMIT_EXCEEDED, ENFORCER);
}

return PolicyResult.allow(PolicyVocab.REASON_WITHIN_RATE_LIMIT, ENFORCER);
}

@Override
public String name() {
return "rate-limit";
}

private static class Entry {
final AtomicInteger count;
final Instant windowReset;

Entry(java.util.function.Supplier<AtomicInteger> supplier, Instant windowReset) {
this.count = supplier.get();
this.windowReset = windowReset;
this.count.set(1);
}
}
}
