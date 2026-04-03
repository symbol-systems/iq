package systems.symbol.policy.impl;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class RoleBasedEnforcerTest {

@Test
void allowsWhenRoleSatisfied() {
RoleBasedEnforcer enforcer = new RoleBasedEnforcer(java.util.Set.of(PolicyVocab.role("admin")));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(PolicyVocab.role("admin")), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);

assertTrue(result.allowed());
}

@Test
void deniesWhenRoleMissing() {
RoleBasedEnforcer enforcer = new RoleBasedEnforcer(java.util.Set.of(PolicyVocab.role("admin")));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:1"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);

assertFalse(result.allowed());
}
}
