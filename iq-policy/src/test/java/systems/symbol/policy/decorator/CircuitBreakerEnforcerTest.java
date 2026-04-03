package systems.symbol.policy.decorator;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerEnforcerTest {

@Test
void opensCircuitAfterFailures() {
I_PolicyEnforcer failing = input -> PolicyResult.deny("deny", null);
CircuitBreakerEnforcer enforcer = new CircuitBreakerEnforcer(failing, 2, Duration.ofSeconds(1));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

assertFalse(enforcer.evaluate(input).allowed());
assertFalse(enforcer.evaluate(input).allowed());
PolicyResult openResult = enforcer.evaluate(input);
assertFalse(openResult.allowed());
assertEquals(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, openResult.reasonIri());
}
}
