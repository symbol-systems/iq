package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.kernel.event.KernelEvent;
import systems.symbol.kernel.event.KernelTopics;
import systems.symbol.kernel.event.NoopEventHub;

/**
 * Lightweight wrapper around a {@link RepositoryConnection} that emits kernel events.
 *
 * <p>Preferred naming: KernelRepositoryConnection.
 */
public class KernelRepositoryConnection implements AutoCloseable {

private final RepositoryConnection delegate;
private final I_EventHub eventHub;

public KernelRepositoryConnection(RepositoryConnection delegate, I_EventHub eventHub) {
this.delegate = delegate;
this.eventHub = eventHub == null ? new NoopEventHub() : eventHub;
}

public static KernelRepositoryConnection wrap(RepositoryConnection connection, I_EventHub eventHub) {
return new KernelRepositoryConnection(connection, eventHub);
}

public RepositoryConnection getDelegate() {
return delegate;
}

public ValueFactory getValueFactory() {
return delegate.getValueFactory();
}

public void add(Statement st, Resource... contexts) throws RepositoryException {
publishEvent(KernelTopics.RDF_STATEMENT_ADD, st.toString());
delegate.add(st, contexts);
}

public void remove(Statement st, Resource... contexts) throws RepositoryException {
publishEvent(KernelTopics.RDF_STATEMENT_REMOVE, st.toString());
delegate.remove(st, contexts);
}

public void clear(Resource... contexts) throws RepositoryException {
publishEvent(KernelTopics.RDF_STATEMENT_CLEAR, "contexts=" + contexts.length);
delegate.clear(contexts);
}

public void commit() throws RepositoryException {
publishEvent(KernelTopics.RDF_REPOSITORY_COMMIT, "committing");
delegate.commit();
}

public void rollback() throws RepositoryException {
publishEvent(KernelTopics.RDF_REPOSITORY_ROLLBACK, "rolling-back");
delegate.rollback();
}

@Override
public void close() {
if (delegate != null && delegate.isOpen()) {
delegate.close();
}
}

private void publishEvent(IRI topic, String message) {
eventHub.publish(KernelEvent.on(topic)
.source(delegate.getValueFactory().createIRI("urn:iq:event:source"))
.contentType("application/json")
.payload("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}")
.build());
}
}
