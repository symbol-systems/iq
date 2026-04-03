package systems.symbol.policy.core;

import org.junit.jupiter.api.Test;
import systems.symbol.kernel.pipeline.KernelCallContext;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class AllowAllEnforcerTest {

@Test
void evaluateReturnsAllow() {
AllowAllEnforcer enforcer = new AllowAllEnforcer();
KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_PRINCIPAL, "alice");
ctx.set(KernelCallContext.KEY_REALM, PolicyVocab.resource("realm:test"));
PolicyInput input = PolicyInput.from(ctx, PolicyVocab.ACTION_READ, PolicyVocab.resource("api/test"));

PolicyResult result = enforcer.evaluate(input);

assertTrue(result.allowed());
assertEquals(PolicyVocab.REASON_COMPOSITE_ALL_ALLOW, result.reasonIri());
}
}
