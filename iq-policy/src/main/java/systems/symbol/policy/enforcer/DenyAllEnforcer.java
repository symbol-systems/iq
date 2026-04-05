package systems.symbol.policy.enforcer;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;

/**
 * Deny-all policy enforcer for lockdown and maintenance windows.
 * Returns deny for every request.
 */
public class DenyAllEnforcer implements I_PolicyEnforcer {

private final String reason;

public DenyAllEnforcer() {
this("deny-all policy active");
}

public DenyAllEnforcer(String reason) {
this.reason = reason;
}

@Override
public PolicyResult evaluate(PolicyInput input) {
return PolicyResult.deny(reason);
}

@Override
public String name() {
return "DenyAllEnforcer";
}

@Override
public int order() {
return 0;
}
}
