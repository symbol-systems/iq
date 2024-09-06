package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.platform.I_Self;

public interface IQ extends I_Self {
ValueFactory vf = SimpleValueFactory.getInstance();
RepositoryConnection getConnection();
void close();
IRI toIRI(String local);
}
