package systems.symbol.policy.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
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

public class GraphPolicyEnforcer implements I_PolicyEnforcer {

private static final Logger log = LoggerFactory.getLogger(GraphPolicyEnforcer.class);
private static final IRI ENFORCER = PolicyVocab.resource("enforcer:graph");

private final Repository repository;
private final IRI adminRole;

public GraphPolicyEnforcer(Repository repository, IRI adminRole) {
this.repository = Objects.requireNonNull(repository, "repository is required");
this.adminRole = adminRole;
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
if (input.principal() == null) {
return PolicyResult.deny(PolicyVocab.REASON_ANONYMOUS_PRINCIPAL, ENFORCER);
}

if (adminRole != null && input.roles().contains(adminRole)) {
return PolicyResult.allow(PolicyVocab.REASON_ADMIN_ROLE_OVERRIDE, ENFORCER);
}

try (RepositoryConnection conn = repository.getConnection()) {
if (conn.hasStatement(input.principal(), PolicyVocab.IS_BLOCKED_FROM, input.realm(), true)) {
return PolicyResult.deny(PolicyVocab.REASON_PRINCIPAL_BLOCKED_FROM_REALM, ENFORCER);
}
if (conn.hasStatement(input.principal(), PolicyVocab.HAS_ACCESS_TO, input.realm(), true)) {
return PolicyResult.allow(PolicyVocab.REASON_PRINCIPAL_HAS_ACCESS_TO_REALM, ENFORCER);
}

conn.getStatements(input.resource(), PolicyVocab.REQUIRES_SCOPE, null, true)
.forEach(stmt -> {
// no op, gather below via stream
});

for (var statement : conn.getStatements(input.resource(), PolicyVocab.REQUIRES_SCOPE, null, true)) {
if (statement.getObject() instanceof IRI scope) {
if (!input.scopes().contains(scope)) {
return PolicyResult.deny("required scope not present: " + scope, ENFORCER);
}
}
if (statement.getObject() instanceof Literal ***REMOVED***Scope) {
IRI requiredScope = PolicyVocab.scope(***REMOVED***Scope.stringValue());
if (!input.scopes().contains(requiredScope)) {
return PolicyResult.deny("required scope not present: " + requiredScope, ENFORCER);
}
}
}

// public realm based on the underlying axiom
Literal trueLit = SimpleValueFactory.getInstance().createLiteral(true);
if (conn.hasStatement(input.realm(), PolicyVocab.IS_PUBLIC, trueLit, true)) {
return PolicyResult.allow(PolicyVocab.REASON_REALM_IS_PUBLIC, ENFORCER);
}
} catch (Exception ex) {
log.error("GraphPolicyEnforcer evaluate failure", ex);
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}

return PolicyResult.deny(PolicyVocab.REASON_NO_MATCHING_ALLOW_POLICY, ENFORCER);
}

@Override
public String name() {
return "graph";
}
}
