package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import static java.util.UUID.randomUUID;

/**
 * For contextual queries
 */
public class IQConnection implements IQStore, AutoCloseable {
    RepositoryConnection connection;
    IRI id;

    public IQConnection(IRI id, RepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
    }

    public IQConnection(String id, RepositoryConnection connection) {
        this.id = connection.getValueFactory().createIRI(id);
        this.connection = connection;
    }

    @Override
    public RepositoryConnection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (connection != null && connection.isOpen())
            connection.close();
    }

    @Override
    public IRI getSelf() {
        return id;
    }

    @Override
    public IRI toIRI(String local) {
        if (!local.contains(":")) {
            return connection.getValueFactory().createIRI(id.stringValue(), local);
        }
        return connection.getValueFactory().createIRI(local);
    }

    public IRI toIRI() {
        return connection.getValueFactory().createIRI(this.id.stringValue(), randomUUID().toString());
    }
}
