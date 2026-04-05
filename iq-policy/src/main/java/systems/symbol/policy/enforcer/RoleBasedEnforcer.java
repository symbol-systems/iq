package systems.symbol.policy.enforcer;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;

/**
 * Role-Based Access Control (RBAC) enforcer.
 * Pure RBAC from JWT roles without SPARQL queries.
 * 
 * Maps role IRIs to action IRIs and evaluates membership.
 */
public class RoleBasedEnforcer implements I_PolicyEnforcer {

private final Map<IRI, Set<IRI>> rolePermissions;

public RoleBasedEnforcer(Map<IRI, Set<IRI>> rolePermissions) {
Objects.requireNonNull(rolePermissions, "rolePermissions is required");
this.rolePermissions = Collections.unmodifiableMap(
new HashMap<>(rolePermissions)
);
}

public RoleBasedEnforcer() {
this.rolePermissions = Collections.emptyMap();
}

@Override
public PolicyResult evaluate(PolicyInput input) {
try {
// If user has no roles, deny
if (input.roles() == null || input.roles().isEmpty()) {
return PolicyResult.deny("no roles assigned");
}

// Check if any user role has permission for the action
for (IRI userRole : input.roles()) {
Set<IRI> allowedActions = rolePermissions.get(userRole);
if (allowedActions != null && allowedActions.contains(input.action())) {
return PolicyResult.allow(
"role " + userRole + " permits " + input.action(),
userRole
);
}
}

return PolicyResult.deny(
"no role permits action " + input.action()
);
} catch (Exception e) {
return PolicyResult.deny("RBAC evaluation failed: " + e.getMessage());
}
}

@Override
public String name() {
return "RoleBasedEnforcer";
}

@Override
public int order() {
return 10;
}
}
