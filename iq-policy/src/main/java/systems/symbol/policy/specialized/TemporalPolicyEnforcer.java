package systems.symbol.policy.specialized;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Instant;
import java.util.Objects;

public class TemporalPolicyEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:temporal");

private final Instant windowStart;
private final Instant windowEnd;

public TemporalPolicyEnforcer(Instant windowStart, Instant windowEnd) {
this.windowStart = Objects.requireNonNull(windowStart, "windowStart is required");
this.windowEnd = Objects.requireNonNull(windowEnd, "windowEnd is required");
if (!windowEnd.isAfter(windowStart)) {
throw new IllegalArgumentException("windowEnd must be after windowStart");
}
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
Instant now = Instant.now();
if (now.isBefore(windowStart)) {
return PolicyResult.deny(PolicyVocab.REASON_BEFORE_ALLOWED_WINDOW, ENFORCER);
}
if (now.isAfter(windowEnd)) {
return PolicyResult.deny(PolicyVocab.REASON_AFTER_ALLOWED_WINDOW, ENFORCER);
}
return PolicyResult.allow(PolicyVocab.REASON_WITHIN_ALLOWED_WINDOW, ENFORCER);
}

@Override
public String name() {
return "temporal";
}
}
