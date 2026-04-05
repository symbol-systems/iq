package systems.symbol.policy.enforcer;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

/**
 * Allow-all policy enforcer for development and open deployments.
 * Returns allow for every request.
 */
public class AllowAllEnforcer implements I_PolicyEnforcer {

@Override
public PolicyResult evaluate(PolicyInput input) {
return PolicyResult.allow("allow-all policy active", PolicyVocab.ACTION_READ);
}

@Override
public String name() {
return "AllowAllEnforcer";
}

@Override
public int order() {
return 0;
}
}
