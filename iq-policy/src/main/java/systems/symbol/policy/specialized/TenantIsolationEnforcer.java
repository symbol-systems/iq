package systems.symbol.policy.specialized;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import java.util.Objects;

public class TenantIsolationEnforcer implements I_PolicyEnforcer {

private static final IRI ENFORCER = PolicyVocab.resource("enforcer:tenant-isolation");
private final org.eclipse.rdf4j.repository.Repository repository;

public TenantIsolationEnforcer(org.eclipse.rdf4j.repository.Repository repository) {
this.repository = Objects.requireNonNull(repository, "repository is required");
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}

var tenantClaim = input.context().getOrDefault("tenant", null);
if (tenantClaim == null && input.realm() != null) {
// realm IRI naming-based fallback
String realmNs = input.realm().stringValue();
tenantClaim = realmNs.contains("tenant") ? realmNs : null;
}

if (tenantClaim == null) {
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}

IRI tenantIri = PolicyVocab.tenant(tenantClaim.toString());

try (RepositoryConnection conn = repository.getConnection()) {
if (conn.hasStatement(input.principal(), PolicyVocab.HAS_ACCESS_TO, tenantIri, true)) {
return PolicyResult.allow(PolicyVocab.REASON_COMPOSITE_ALL_ALLOW, ENFORCER);
}
return PolicyResult.deny(PolicyVocab.REASON_PRINCIPAL_MISSING_REQUIRED_ROLE, ENFORCER);
} catch (Exception ex) {
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}
}
}
