package systems.symbol.policy.decorator;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreakerEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:circuit-breaker");

private final I_PolicyEnforcer delegate;
private final int failureThreshold;
private final Duration resetTimeout;

private final AtomicInteger failureCount = new AtomicInteger(0);
private final AtomicReference<Instant> openAt = new AtomicReference<>(null);

public CircuitBreakerEnforcer(I_PolicyEnforcer delegate, int failureThreshold, Duration resetTimeout) {
this.delegate = Objects.requireNonNull(delegate, "delegate is required");
if (failureThreshold <= 0) {
throw new IllegalArgumentException("failureThreshold must be positive");
}
this.failureThreshold = failureThreshold;
this.resetTimeout = Objects.requireNonNull(resetTimeout, "resetTimeout is required");
}

@Override
public PolicyResult evaluate(PolicyInput input) {
Instant now = Instant.now();
Instant opened = openAt.get();
if (opened != null) {
if (now.isBefore(opened.plus(resetTimeout))) {
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}
if (openAt.compareAndSet(opened, null)) {
failureCount.set(0);
}
}

PolicyResult result = delegate.evaluate(input);
if (result == null || !result.allowed()) {
if (failureCount.incrementAndGet() >= failureThreshold) {
openAt.compareAndSet(null, now);
}
return result == null ? PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER) : result;
}

failureCount.set(0);
return result;
}

@Override
public String name() {
return "circuit-breaker";
}
}
