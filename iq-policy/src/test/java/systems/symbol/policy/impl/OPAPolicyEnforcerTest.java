package systems.symbol.policy.impl;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class OPAPolicyEnforcerTest {

@Test
void deniesWhenOPAUnreachableAndFailClosed() {
OPAPolicyEnforcer enforcer = new OPAPolicyEnforcer("http://127.0.0.1:1/v1/data/policy", java.time.Duration.ofSeconds(1), "closed");
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

assertFalse(enforcer.evaluate(input).allowed());
}

@Test
void allowsWhenOPAUnreachableAndFailOpen() {
OPAPolicyEnforcer enforcer = new OPAPolicyEnforcer("http://127.0.0.1:1/v1/data/policy", java.time.Duration.ofSeconds(1), "open");
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

assertTrue(enforcer.evaluate(input).allowed());
}
}
