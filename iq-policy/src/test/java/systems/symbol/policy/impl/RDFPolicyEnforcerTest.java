package systems.symbol.policy.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

import static org.junit.jupiter.api.Assertions.*;

class RDFPolicyEnforcerTest {

@Test
void blockedPrincipalIsDenied() {
Repository repo = new SailRepository(new MemoryStore());
repo.init();
IRI alice = PolicyVocab.principal("alice");
IRI realm = PolicyVocab.resource("realm:public");
try (RepositoryConnection conn = repo.getConnection()) {
conn.add(alice, PolicyVocab.IS_BLOCKED_FROM, realm);
}

RDFPolicyEnforcer enforcer = new RDFPolicyEnforcer(repo, PolicyVocab.role("admin"));
PolicyInput input = new PolicyInput(alice, realm, PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());
PolicyResult result = enforcer.evaluate(input);

assertFalse(result.allowed());
assertEquals(PolicyVocab.REASON_PRINCIPAL_BLOCKED_FROM_REALM, result.reasonIri());
}

@Test
void publicRealmIsAllowed() {
Repository repo = new SailRepository(new MemoryStore());
repo.init();
IRI realm = PolicyVocab.resource("realm:public");
try (RepositoryConnection conn = repo.getConnection()) {
conn.add(realm, PolicyVocab.IS_PUBLIC, SimpleValueFactory.getInstance().createLiteral(true));
}

IRI alice = PolicyVocab.principal("alice");
RDFPolicyEnforcer enforcer = new RDFPolicyEnforcer(repo, null);
PolicyInput input = new PolicyInput(alice, realm, PolicyVocab.ACTION_READ, PolicyVocab.resource("api"), java.util.Set.of(), java.util.Set.of(), java.util.Map.of());
PolicyResult result = enforcer.evaluate(input);

assertTrue(result.allowed());
assertEquals(PolicyVocab.REASON_REALM_IS_PUBLIC, result.reasonIri());
}
}
