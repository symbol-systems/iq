package systems.symbol.kernel.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.I_Agent;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryTest {

    static class TestAgent implements I_Agent {
        private final IRI self;

        TestAgent(String iri) {
            this.self = SimpleValueFactory.getInstance().createIRI(iri);
        }

        @Override
        public IRI getSelf() {
            return self;
        }

        @Override public void start() {
        }

        @Override public void stop() {
        }

        @Override public org.eclipse.rdf4j.model.Model getThoughts() {
            return null;
        }

        @Override public systems.symbol.fsm.I_StateMachine<org.eclipse.rdf4j.model.Resource> getStateMachine() {
            return null;
        }
    }

    @Test
    void registryRoundTrip() {
        I_AgentRegistry registry = new SimpleAgentRegistry();
        TestAgent a1 = new TestAgent("urn:agent:1");

        assertEquals(Optional.empty(), registry.register(null));
        assertEquals(Optional.empty(), registry.lookup(null));

        Optional<I_Agent> previous = registry.register(a1);
        assertTrue(previous.isEmpty());

        Optional<I_Agent> found = registry.lookup(a1.getSelf());
        assertTrue(found.isPresent());
        assertSame(a1, found.get());

        Collection<I_Agent> all = registry.list();
        assertEquals(1, all.size());

        Optional<I_Agent> removed = registry.unregister(a1.getSelf());
        assertTrue(removed.isPresent());
        assertEquals(0, registry.size());
    }
}
