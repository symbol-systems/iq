package systems.symbol.policy.core;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;
import org.eclipse.rdf4j.model.IRI;

public class DenyAllEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:deny-all");

@Override
public PolicyResult evaluate(systems.symbol.kernel.policy.PolicyInput input) {
return PolicyResult.deny(PolicyVocab.REASON_DENY_ALL_POLICY, ENFORCER);
}

@Override
public String name() {
return "deny-all";
}
}
