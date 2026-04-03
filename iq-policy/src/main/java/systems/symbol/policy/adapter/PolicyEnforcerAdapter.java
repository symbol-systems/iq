package systems.symbol.policy.adapter;

import systems.symbol.kernel.pipeline.I_AccessPolicy;
import systems.symbol.kernel.pipeline.KernelCallContext;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyVocab;

public class PolicyEnforcerAdapter implements I_AccessPolicy {

private final systems.symbol.kernel.policy.I_PolicyEnforcer enforcer;

public PolicyEnforcerAdapter(systems.symbol.kernel.policy.I_PolicyEnforcer enforcer) {
this.enforcer = enforcer;
}

@Override
public boolean allows(KernelCallContext ctx) {
if (ctx == null) {
return false;
}
var input = PolicyInput.from(ctx, PolicyVocab.ACTION_EXECUTE, PolicyVocab.resource("pipeline"));
var result = enforcer.evaluate(input);
return result != null && result.allowed();
}
}
