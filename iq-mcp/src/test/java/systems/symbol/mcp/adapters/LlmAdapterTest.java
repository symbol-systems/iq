package systems.symbol.mcp.adapters;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.realm.I_Realm;
import java.security.KeyPair;

class LlmAdapterTest {
static class SimpleRealm implements I_Realm {
private final IRI id;

SimpleRealm(String uri) {
this.id = SimpleValueFactory.getInstance().createIRI(uri);
}

@Override
public IRI getSelf() {
return id;
}

@Override
public KeyPair keys() {
return null;
}

@Override
public org.eclipse.rdf4j.model.Model getModel() {
return new LinkedHashModel();
}

@Override
public org.eclipse.rdf4j.repository.Repository getRepository() {
return null;
}

public org.apache.commons.vfs2.FileObject toFile(org.eclipse.rdf4j.model.IRI iri) {
throw new UnsupportedOperationException();
}

@Override
public systems.symbol.secrets.I_Secrets getSecrets() {
return null;
}

public org.apache.commons.vfs2.FileObject locate(org.eclipse.rdf4j.model.IRI iri) throws java.net.URISyntaxException, org.apache.commons.vfs2.FileSystemException {
throw new UnsupportedOperationException();
}
}

@Test
void testEchoCompletion() throws Exception {
SimpleRealm r = new SimpleRealm("urn:realm:test");
LlmAdapter a = new LlmAdapter("urn:adapter:llm", r);
ValueFactory vf = SimpleValueFactory.getInstance();

Model in = new LinkedHashModel();
in.add(vf.createIRI("urn:in:1"), LlmAdapter.PROMPT_PREDICATE, vf.createLiteral("hello world"));

systems.symbol.mcp.I_MCPResult r1 = a.invoke(vf.createIRI("urn:tool:llm"), in);
assertTrue(r1.isSuccess());
boolean hasCompletion = r1.getPayload().stream().anyMatch(st -> st.getPredicate().getLocalName().equals("completion"));
assertTrue(hasCompletion);
}
}
