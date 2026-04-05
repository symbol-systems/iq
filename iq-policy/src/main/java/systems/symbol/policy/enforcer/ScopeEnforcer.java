package systems.symbol.policy.enforcer;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import org.eclipse.rdf4j.model.IRI;

/**
 * Scope-based access control enforcer.
 * Validates that caller's scopes satisfy resource requirements.
 */
public class ScopeEnforcer implements I_PolicyEnforcer {

public enum ScopeMatch {
ANY,   // One scope suffices
ALL// All required scopes must be present
}

private final Map<IRI, Set<IRI>> resourceScopes;
private final ScopeMatch matchMode;

public ScopeEnforcer(Map<IRI, Set<IRI>> resourceScopes, ScopeMatch matchMode) {
Objects.requireNonNull(resourceScopes, "resourceScopes is required");
Objects.requireNonNull(matchMode, "matchMode is required");
this.resourceScopes = Collections.unmodifiableMap(
new HashMap<>(resourceScopes)
);
this.matchMode = matchMode;
}

public ScopeEnforcer(Map<IRI, Set<IRI>> resourceScopes) {
this(resourceScopes, ScopeMatch.ANY);
}

@Override
public PolicyResult evaluate(PolicyInput input) {
try {
// Get required scopes for this resource
Set<IRI> requiredScopes = resourceScopes.get(input.resource());

// No scope requirement means allow
if (requiredScopes == null || requiredScopes.isEmpty()) {
return PolicyResult.allow("no scope requirement", null);
}

// Caller has no scopes - deny
if (input.scopes() == null || input.scopes().isEmpty()) {
return PolicyResult.deny("no scopes in request");
}

if (matchMode == ScopeMatch.ALL) {
// Check all required scopes are present
if (input.scopes().containsAll(requiredScopes)) {
return PolicyResult.allow(
"all required scopes present",
null
);
} else {
return PolicyResult.deny(
"missing required scopes: " + requiredScopes
);
}
} else { // ANY
// Check at least one required scope is present
for (IRI requiredScope : requiredScopes) {
if (input.scopes().contains(requiredScope)) {
return PolicyResult.allow(
"scope " + requiredScope + " matched",
requiredScope
);
}
}
return PolicyResult.deny(
"none of required scopes present: " + requiredScopes
);
}
} catch (Exception e) {
return PolicyResult.deny("scope evaluation failed: " + e.getMessage());
}
}

@Override
public String name() {
return "ScopeEnforcer";
}

@Override
public int order() {
return 5;
}
}
