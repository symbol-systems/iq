package systems.symbol.kernel.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyVocabTest {

@Test
void principalCreatesIri() {
var iri = PolicyVocab.principal("alice");
assertNotNull(iri);
assertEquals("urn:iq:principal:alice", iri.stringValue());
}

@Test
void resourceCreatesIri() {
var iri = PolicyVocab.resource("api/chat");
assertNotNull(iri);
assertEquals("urn:iq:resource:api/chat", iri.stringValue());
}

@Test
void connectorOpCreatesIri() {
var iri = PolicyVocab.connectorOp("aws", "list");
assertEquals("urn:iq:connector:aws:list", iri.stringValue());
}

@Test
void roleCreatesIri() {
assertEquals("urn:iq:role:admin", PolicyVocab.role("admin").stringValue());
}

@Test
void scopeCreatesIri() {
assertEquals("urn:iq:policy:scope:chat.read", PolicyVocab.scope("chat.read").stringValue());
}
}
