package systems.symbol.rdf4j.iq;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.HashMap;
import java.util.Map;

import systems.symbol.rdf4j.store.IQStore;

public class KBMS implements IQStore {
private IRI self;
private RepositoryConnection connection;

public KBMS(IRI self, RepositoryConnection connection) {
this.self = self;
this.connection = connection;
}

@Override
public IRI getSelf() {
return self;
}

@Override
public RepositoryConnection getConnection() {
return connection;
}

@Override
public void close() {
try {
if (connection != null && connection.isOpen()) {
connection.close();
}
} catch (Exception e) {
// noop
}
}

@Override
public IRI toIRI(String value) {
return connection.getValueFactory().createIRI(value);
}

public Map<String, Object> getContext() {
return new HashMap<>();
}
}
