package systems.symbol.mcp.adapters;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.realm.I_Realm;
import java.security.KeyPair;

class FactAdapterTest {
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
    void testEchoesLiterals() throws Exception {
        SimpleRealm r = new SimpleRealm("urn:realm:test");
        FactAdapter a = new FactAdapter("urn:adapter:fact", r);
        ValueFactory vf = SimpleValueFactory.getInstance();
        Model in = new LinkedHashModel();
        in.add(vf.createIRI("urn:test:sub"), vf.createIRI("urn:test:pred"), vf.createLiteral("alpha"));

        I_MCPResultWrapper res = new I_MCPResultWrapper(a.invoke(vf.createIRI("urn:tool:test"), in));
        assertTrue(res.isSuccess());
        boolean found = false;
        for (Statement st : res.getPayload()) {
            if (st.getPredicate().getLocalName().equals("result") && st.getObject() instanceof Literal) {
                found = true;
            }
        }
        assertTrue(found);
    }

    static class I_MCPResultWrapper {
        private final org.eclipse.rdf4j.model.Model payload;
        private final boolean ok;

        I_MCPResultWrapper(systems.symbol.mcp.I_MCPResult r) {
            this.ok = r.isSuccess();
            this.payload = r.getPayload();
        }

        boolean isSuccess() { return ok; }
        org.eclipse.rdf4j.model.Model getPayload() { return payload; }
    }
}
