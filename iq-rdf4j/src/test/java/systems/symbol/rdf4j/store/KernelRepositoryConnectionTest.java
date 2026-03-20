package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.kernel.event.KernelEvent;
import systems.symbol.kernel.event.KernelTopics;
import systems.symbol.kernel.event.SimpleEventHub;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KernelRepositoryConnectionTest {

@Test
void addStatementEmitsRdfStatementAddEvent() {
SailRepository repository = new SailRepository(new MemoryStore());
repository.init();

I_EventHub hub = new SimpleEventHub();
List<KernelEvent> events = new ArrayList<>();
hub.subscribe(KernelTopics.RDF_STATEMENT_ADD, events::add);

try (RepositoryConnection conn = repository.getConnection();
 KernelRepositoryConnection hookConn = KernelRepositoryConnection.wrap(conn, hub)) {

IRI subj = hookConn.getValueFactory().createIRI("urn:subject");
IRI pred = hookConn.getValueFactory().createIRI("urn:predicate");
IRI obj = hookConn.getValueFactory().createIRI("urn:object");

Statement st = hookConn.getValueFactory().createStatement(subj, pred, obj);
hookConn.add(st);
hookConn.commit();
}

assertEquals(1, events.size(), "Expect one RDF statement add event");
assertEquals(KernelTopics.RDF_STATEMENT_ADD, events.get(0).getTopic());
}
}
