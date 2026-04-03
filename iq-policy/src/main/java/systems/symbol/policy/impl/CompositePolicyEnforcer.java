package systems.symbol.policy.impl;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;
import java.util.Objects;

public class CompositePolicyEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:composite");

private final List<I_PolicyEnforcer> enforcers;

public CompositePolicyEnforcer(List<I_PolicyEnforcer> enforcers) {
Objects.requireNonNull(enforcers, "enforcers must not be null");
this.enforcers = List.copyOf(enforcers);
}

@Override
public PolicyResult evaluate(PolicyInput input) {
PolicyResult abstain = null;
for (I_PolicyEnforcer enforcer : enforcers) {
PolicyResult result = enforcer.evaluate(input);
if (result == null) {
continue;
}
if (result.isAbstain()) {
abstain = result;
continue;
}
if (!result.allowed()) {
return result;
}
// allow continues to next enforcement layer
}
if (abstain != null) {
// Proposed semantics: if all abstain, deny by default.
return PolicyResult.deny(PolicyVocab.REASON_ALL_ENFORCERS_ABSTAINED, ENFORCER);
}
return PolicyResult.allow(PolicyVocab.REASON_COMPOSITE_ALL_ALLOW, ENFORCER);
}

@Override
public String name() {
return "composite";
}
}
