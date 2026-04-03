package systems.symbol.policy.specialized;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TemporalPolicyEnforcerTest {

@Test
void allowsWithinWindow() {
var now = Instant.now();
TemporalPolicyEnforcer enforcer = new TemporalPolicyEnforcer(now.minusSeconds(1), now.plusSeconds(60));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);
assertTrue(result.allowed());
}

@Test
void deniesOutsideWindow() {
var now = Instant.now();
TemporalPolicyEnforcer enforcer = new TemporalPolicyEnforcer(now.minusSeconds(60), now.minusSeconds(1));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);
assertFalse(result.allowed());
}
}
