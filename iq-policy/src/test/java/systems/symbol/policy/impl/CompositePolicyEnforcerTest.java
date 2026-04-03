package systems.symbol.policy.impl;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class CompositePolicyEnforcerTest {

record PassEnforcer() implements I_PolicyEnforcer {
@Override
public PolicyResult evaluate(PolicyInput input) {
return PolicyResult.allow("pass", null);
}
}

record DenyEnforcer() implements I_PolicyEnforcer {
@Override
public PolicyResult evaluate(PolicyInput input) {
return PolicyResult.deny("deny", null);
}
}

record AbstainEnforcer() implements I_PolicyEnforcer {
@Override
public PolicyResult evaluate(PolicyInput input) {
return PolicyResult.abstain("abstain");
}
}

@Test
void firstDenyWins() {
CompositePolicyEnforcer composite = new CompositePolicyEnforcer(java.util.List.of(new PassEnforcer(), new DenyEnforcer()));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:public"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = composite.evaluate(input);

assertFalse(result.allowed());
assertNull(result.reasonIri());
assertTrue(result.reason().contains("deny"));
}

@Test
void allAbstainDenies() {
CompositePolicyEnforcer composite = new CompositePolicyEnforcer(java.util.List.of(new AbstainEnforcer(), new AbstainEnforcer()));
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), PolicyVocab.resource("realm:public"), PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = composite.evaluate(input);

assertFalse(result.allowed());
assertEquals(PolicyVocab.REASON_ALL_ENFORCERS_ABSTAINED, result.reasonIri());
assertTrue(result.reason().contains("all-enforcers-abstained") || result.reason().contains("all enforcers abstained"));
}
}
