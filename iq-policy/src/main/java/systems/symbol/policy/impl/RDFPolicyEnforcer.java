package systems.symbol.policy.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Objects;

public class RDFPolicyEnforcer implements I_PolicyEnforcer {

private static final Logger log = LoggerFactory.getLogger(RDFPolicyEnforcer.class);
private static final IRI ENFORCER = PolicyVocab.resource("enforcer:rdf-acl");

private final Repository repository;
private final IRI adminRole;

public RDFPolicyEnforcer(Repository repository, IRI adminRole) {
this.repository = Objects.requireNonNull(repository, "repository is required");
this.adminRole = adminRole;
}

@Override
public PolicyResult evaluate(PolicyInput input) {
try {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
if (input.principal() == null) {
return PolicyResult.deny(PolicyVocab.REASON_ANONYMOUS_PRINCIPAL, ENFORCER);
}

// admin override
if (adminRole != null && input.roles().contains(adminRole)) {
return PolicyResult.allow(PolicyVocab.REASON_ADMIN_ROLE_OVERRIDE, ENFORCER);
}

try (RepositoryConnection conn = repository.getConnection()) {
// explicit block wins
if (conn.hasStatement(input.principal(), PolicyVocab.IS_BLOCKED_FROM, input.realm(), true)) {
return PolicyResult.deny(PolicyVocab.REASON_PRINCIPAL_BLOCKED_FROM_REALM, ENFORCER);
}

// explicit grant
if (conn.hasStatement(input.principal(), PolicyVocab.HAS_ACCESS_TO, input.realm(), true)) {
return PolicyResult.allow(PolicyVocab.REASON_PRINCIPAL_HAS_ACCESS_TO_REALM, ENFORCER);
}

// public realm
Literal trueLit = SimpleValueFactory.getInstance().createLiteral(true);
if (conn.hasStatement(input.realm(), PolicyVocab.IS_PUBLIC, trueLit, true)) {
return PolicyResult.allow(PolicyVocab.REASON_REALM_IS_PUBLIC, ENFORCER);
}
}

return PolicyResult.deny(PolicyVocab.REASON_NO_MATCHING_ALLOW_POLICY, ENFORCER);
} catch (Exception ex) {
log.error("RDFPolicyEnforcer evaluate failed", ex);
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}
}

@Override
public String name() {
return "rdf";
}
}
