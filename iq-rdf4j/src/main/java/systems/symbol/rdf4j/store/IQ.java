package systems.symbol.rdf4j.store;

import systems.symbol.model.I_Self;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public interface IQ extends I_Self {
ValueFactory vf = SimpleValueFactory.getInstance();
RepositoryConnection getConnection();
void close();
IRI toIRI(String local);



}
