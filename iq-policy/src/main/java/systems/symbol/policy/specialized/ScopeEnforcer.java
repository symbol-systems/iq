package systems.symbol.policy.specialized;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class ScopeEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:scope");

private final Set<IRI> requiredScopes;

public ScopeEnforcer(Set<IRI> requiredScopes) {
this.requiredScopes = requiredScopes == null ? Collections.emptySet() : Set.copyOf(requiredScopes);
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
if (input.principal() == null) {
return PolicyResult.deny(PolicyVocab.REASON_ANONYMOUS_PRINCIPAL, ENFORCER);
}
if (requiredScopes.isEmpty()) {
return PolicyResult.allow(PolicyVocab.REASON_NO_SCOPES_REQUIRED, ENFORCER);
}
if (input.scopes().containsAll(requiredScopes)) {
return PolicyResult.allow(PolicyVocab.REASON_PRINCIPAL_HAS_ACCESS_TO_REALM, ENFORCER);
}
return PolicyResult.deny(PolicyVocab.REASON_PRINCIPAL_MISSING_REQUIRED_SCOPE, ENFORCER);
}

@Override
public String name() {
return "scope";
}
}
