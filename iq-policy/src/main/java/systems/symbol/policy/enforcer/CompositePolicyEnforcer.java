package systems.symbol.policy.enforcer;

import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Composite policy enforcer that chains multiple enforcers.
 * 
 * Semantics (deny-wins):
 * - First DENY → short-circuit and return deny
 * - If all enforcers return ABSTAIN (not supported) → deny
 * - First ALLOW after all abstains → allow
 * 
 * Enforcers are ordered by I_PolicyEnforcer.order()
 */
public class CompositePolicyEnforcer implements I_PolicyEnforcer {

private final List<I_PolicyEnforcer> enforcers;

public CompositePolicyEnforcer(List<I_PolicyEnforcer> enforcers) {
Objects.requireNonNull(enforcers, "enforcers list is required");
if (enforcers.isEmpty()) {
throw new IllegalArgumentException("enforcers list cannot be empty");
}
// Sort by order
this.enforcers = enforcers.stream()
.sorted(Comparator.comparingInt(I_PolicyEnforcer::order))
.toList();
}

@Override
public PolicyResult evaluate(PolicyInput input) {
try {
for (I_PolicyEnforcer enforcer : enforcers) {
PolicyResult result = enforcer.evaluate(input);

// First deny → short-circuit
if (!result.allowed()) {
return result;
}

// First allow → return it (don't continue)
if (result.allowed()) {
return result;
}
}

// All enforcers abstained or no enforcers - deny by default
return PolicyResult.deny("no enforcer granted access");
} catch (Exception e) {
return PolicyResult.deny("composite evaluation failed: " + e.getMessage());
}
}

@Override
public String name() {
return "CompositePolicyEnforcer[" + enforcers.size() + "]";
}

@Override
public int order() {
return 0;
}
}
