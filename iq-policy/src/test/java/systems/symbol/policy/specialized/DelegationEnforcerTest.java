package systems.symbol.policy.specialized;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class DelegationEnforcerTest {

@Test
void abstainsWhenNoDelegation() {
DelegationEnforcer enforcer = new DelegationEnforcer();
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);
assertFalse(result.allowed());
assertTrue(result.isAbstain());
}

@Test
void allowsWhenDelegationHeaderPresent() {
var context = java.util.Map.<String,Object>of("delegated_by", "urn:iq:principal:bob");
DelegationEnforcer enforcer = new DelegationEnforcer();
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), context);

PolicyResult result = enforcer.evaluate(input);
assertTrue(result.allowed());
}
}
