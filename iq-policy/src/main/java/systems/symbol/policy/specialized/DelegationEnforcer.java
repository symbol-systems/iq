package systems.symbol.policy.specialized;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Objects;

public class DelegationEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:delegation");

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
Object delegate = input.context().get("delegated_by");
if (delegate instanceof String || delegate instanceof IRI) {
return PolicyResult.allow(PolicyVocab.REASON_COMPOSITE_ALL_ALLOW, ENFORCER);
}
return PolicyResult.abstain("abstain");
}

@Override
public String name() {
return "delegation";
}
}
