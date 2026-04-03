package systems.symbol.policy.decorator;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Objects;

public class AuditingPolicyEnforcer implements I_PolicyEnforcer {

private static final Logger log = LoggerFactory.getLogger(AuditingPolicyEnforcer.class);
private static final IRI ENFORCER = PolicyVocab.resource("enforcer:auditing");

private final I_PolicyEnforcer delegate;

public AuditingPolicyEnforcer(I_PolicyEnforcer delegate) {
this.delegate = Objects.requireNonNull(delegate, "delegate is required");
}

@Override
public PolicyResult evaluate(PolicyInput input) {
PolicyResult result = delegate.evaluate(input);
log.info("policy_eval action={} resource={} principal={} result={} reasonIri={} reason={}",
input.action(), input.resource(), input.principal(),
result == null ? "null" : result.allowed(),
result == null ? "null-result" : result.reasonIri(),
result == null ? "null-result" : result.reason());

if (result == null) {
return PolicyResult.deny("delegate returned null", ENFORCER);
}
return result;
}

@Override
public String name() {
return "auditing";
}
}
