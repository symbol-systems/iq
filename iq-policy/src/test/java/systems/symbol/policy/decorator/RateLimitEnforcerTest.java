package systems.symbol.policy.decorator;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitEnforcerTest {

@Test
void deniesWhenOverLimit() {
RateLimitEnforcer enforcer = new RateLimitEnforcer(1, Duration.ofSeconds(60));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

assertTrue(enforcer.evaluate(input).allowed());
assertFalse(enforcer.evaluate(input).allowed());
}
}
