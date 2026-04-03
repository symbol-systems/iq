package systems.symbol.policy.decorator;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class CachingPolicyEnforcerTest {

@Test
void cachesResultsByInputKey() {
var delegate = new systems.symbol.kernel.policy.I_PolicyEnforcer() {
int count = 0;

@Override
public PolicyResult evaluate(PolicyInput input) {
count++;
return PolicyResult.allow("count=" + count, null);
}
};

CachingPolicyEnforcer enforcer = new CachingPolicyEnforcer(delegate);
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult r1 = enforcer.evaluate(input);
PolicyResult r2 = enforcer.evaluate(input);

assertEquals(r1.reason(), r2.reason());
}
}
