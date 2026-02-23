package systems.symbol.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.realm.I_Realm;
import java.security.KeyPair;
import java.util.Optional;

class MCPRealmServiceTest {

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
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SimpleRealm r = (SimpleRealm) o;
            return id.equals(r.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        // Minimal interface methods omitted in test (I_Realm may have more),
        // but tests only rely on getSelf(), equals and hashCode.

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

        public org.apache.commons.vfs2.FileObject locate(org.eclipse.rdf4j.model.IRI iri) throws java.net.URISyntaxException, org.apache.commons.vfs2.FileSystemException {
            throw new UnsupportedOperationException();
        }

        @Override
        public systems.symbol.secrets.I_Secrets getSecrets() {
            return null;
        }

        public org.apache.commons.vfs2.FileObject toFile(org.eclipse.rdf4j.model.IRI iri) {
            throw new UnsupportedOperationException();
        }
    }

    static class MockManifest implements I_MCPToolManifest {
        private final IRI self;
        private final String name;

        MockManifest(String adapter, String name) {
            this.self = SimpleValueFactory.getInstance().createIRI(adapter + "#" + name);
            this.name = name;
        }

        @Override
        public IRI getSelf() {
            return self;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "mock";
        }


        @Override
        public Model getInputShape() {
            return new LinkedHashModel();
        }

        @Override
        public Model getOutputShape() {
            return new LinkedHashModel();
        }

        @Override
        public String getAuthorizationQuery() {
            return null;
        }

        @Override
        public int getRateLimit() {
            return -1;
        }

        @Override
        public int getCost() {
            return 0;
        }
    }

    static class MockResult implements I_MCPResult {
        private final boolean ok;

        MockResult(boolean ok) {
            this.ok = ok;
        }

        @Override
        public boolean isSuccess() {
            return ok;
        }

        @Override
        public Model getPayload() {
            return new LinkedHashModel();
        }

        @Override
        public Model getAudit() {
            return new LinkedHashModel();
        }

        @Override
        public java.util.Optional<org.eclipse.rdf4j.model.IRI> getError() { return java.util.Optional.empty(); }

        @Override
        public java.util.Optional<Throwable> getCause() { return java.util.Optional.empty(); }

        @Override
        public int getCost() { return 0; }

        @Override
        public long getDurationMillis() { return 0L; }
    }

    static class MockAdapter implements I_MCPAdapter {
        private final IRI self;
        private final I_Realm realm;
        private final Set<I_MCPToolManifest> tools = new HashSet<>();
        volatile boolean invoked = false;

        MockAdapter(String id, I_Realm realm, String... toolNames) {
            this.self = SimpleValueFactory.getInstance().createIRI(id);
            this.realm = realm;
            for (String t : toolNames) {
                tools.add(new MockManifest(id, t));
            }
        }

        @Override
        public IRI getSelf() {
            return self;
        }

        @Override
        public I_Realm getRealm() {
            return realm;
        }

        @Override
        public I_MCPResult invoke(IRI toolName, Model inputModel) {
            invoked = true;
            return new MockResult(true);
        }

        @Override
        public java.util.Collection<I_MCPToolManifest> listTools() {
            return Collections.unmodifiableSet(tools);
        }

        @Override
        public I_MCPToolManifest getTool(String toolName) {
            return tools.stream().filter(t -> t.getName().equals(toolName)).findFirst().orElse(null);
        }
    }

    @Test
    void testRegisterAndList() {
        MCPRealmService server = new MCPRealmService();
        SimpleRealm r1 = new SimpleRealm("urn:realm:one");
        MockAdapter a1 = new MockAdapter("urn:adapter:1", r1, "test.tool");

        assertTrue(server.registerAdapter(a1));
        Collection<I_MCPAdapter> adapters = server.getAdapters();
        assertEquals(1, adapters.size());
        assertTrue(adapters.contains(a1));

        // duplicate register returns false
        assertFalse(server.registerAdapter(a1));

        // unregister
        assertTrue(server.unregisterAdapter(a1));
        assertFalse(server.getAdapters().contains(a1));
    }

    @Test
    void testAdapterSelectionByRealm() throws Exception {
        MCPRealmService server = new MCPRealmService();
        SimpleRealm r1 = new SimpleRealm("urn:realm:one");
        SimpleRealm r2 = new SimpleRealm("urn:realm:two");

        MockAdapter a1 = new MockAdapter("urn:adapter:1", r1, "test.tool");
        MockAdapter a2 = new MockAdapter("urn:adapter:2", r2, "test.tool");

        server.registerAdapter(a1);
        server.registerAdapter(a2);

        // when preferring realm r2 we get a2
        Optional<I_MCPAdapter> chosen = server.getAdapterForTool("test.tool", r2);
        assertTrue(chosen.isPresent());
        assertEquals(a2, chosen.get());

        // invoking with explicit realm invokes the expected adapter
        Model m = new LinkedHashModel();
        server.invokeTool("test.tool", m, r2);
        assertTrue(a2.invoked);
        assertFalse(a1.invoked);
    }
}
