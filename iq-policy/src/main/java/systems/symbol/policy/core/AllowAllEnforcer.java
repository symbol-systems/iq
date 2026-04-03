package systems.symbol.policy.core;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;
import org.eclipse.rdf4j.model.IRI;

public class AllowAllEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:allow-all");

@Override
public PolicyResult evaluate(systems.symbol.kernel.policy.PolicyInput input) {
return PolicyResult.allow(PolicyVocab.REASON_COMPOSITE_ALL_ALLOW, ENFORCER);
}

@Override
public String name() {
return "allow-all";
}
}
