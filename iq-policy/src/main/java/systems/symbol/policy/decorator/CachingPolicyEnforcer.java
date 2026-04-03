package systems.symbol.policy.decorator;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingPolicyEnforcer implements I_PolicyEnforcer {

private final I_PolicyEnforcer delegate;
private final ConcurrentMap<Integer, PolicyResult> cache;

public CachingPolicyEnforcer(I_PolicyEnforcer delegate) {
this.delegate = Objects.requireNonNull(delegate, "delegate is required");
this.cache = new ConcurrentHashMap<>();
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, PolicyVocab.resource("enforcer:caching"));
}
int key = computeKey(input);
return cache.computeIfAbsent(key, k -> delegate.evaluate(input));
}

@Override
public String name() {
return "caching";
}

private int computeKey(PolicyInput input) {
return Objects.hash(
input.principal(),
input.realm(),
input.action(),
input.resource(),
input.roles(),
input.scopes());
}
}
