package systems.symbol.policy.impl;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class RoleBasedEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:rbac");

private final Set<IRI> requiredRoles;

public RoleBasedEnforcer(Set<IRI> requiredRoles) {
this.requiredRoles = requiredRoles == null ? Collections.emptySet() : Set.copyOf(requiredRoles);
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
if (input.principal() == null) {
return PolicyResult.deny(PolicyVocab.REASON_ANONYMOUS_PRINCIPAL, ENFORCER);
}
if (requiredRoles.isEmpty()) {
return PolicyResult.allow(PolicyVocab.REASON_NO_ROLES_REQUIRED, ENFORCER);
}
for (IRI role : input.roles()) {
if (requiredRoles.contains(role)) {
return PolicyResult.allow(PolicyVocab.REASON_PRINCIPAL_HAS_ACCESS_TO_REALM, ENFORCER);
}
}
return PolicyResult.deny(PolicyVocab.REASON_PRINCIPAL_MISSING_REQUIRED_ROLE, ENFORCER);
}

@Override
public String name() {
return "rbac";
}
}
