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

class GraphPolicyEnforcerTest {

@Test
void grantsWhenResourceRequirementMet() {
Repository repo = new SailRepository(new MemoryStore());
repo.init();
IRI resource = PolicyVocab.resource("api:123");
IRI realm = PolicyVocab.resource("realm:1");
try (RepositoryConnection conn = repo.getConnection()) {
conn.add(resource, PolicyVocab.REQUIRES_SCOPE, PolicyVocab.scope("chat.read"));
conn.add(realm, PolicyVocab.IS_PUBLIC, SimpleValueFactory.getInstance().createLiteral(true));
}

GraphPolicyEnforcer enforcer = new GraphPolicyEnforcer(repo, null);
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), realm, PolicyVocab.ACTION_READ, resource, java.util.Set.of(PolicyVocab.scope("chat.read")), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);
assertTrue(result.allowed());
}

@Test
void deniesWhenResourceRequirementMissingScope() {
Repository repo = new SailRepository(new MemoryStore());
repo.init();
IRI resource = PolicyVocab.resource("api:123");
IRI realm = PolicyVocab.resource("realm:1");
try (RepositoryConnection conn = repo.getConnection()) {
conn.add(resource, PolicyVocab.REQUIRES_SCOPE, PolicyVocab.scope("chat.read"));
}

GraphPolicyEnforcer enforcer = new GraphPolicyEnforcer(repo, null);
PolicyInput input = new PolicyInput(PolicyVocab.principal("alice"), realm, PolicyVocab.ACTION_READ, resource, java.util.Set.of(), java.util.Set.of(), java.util.Map.of());

PolicyResult result = enforcer.evaluate(input);
assertFalse(result.allowed());
}
}
