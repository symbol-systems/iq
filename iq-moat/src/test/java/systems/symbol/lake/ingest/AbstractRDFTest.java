package systems.symbol.lake.ingest;

import systems.symbol.COMMONS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import java.io.File;
import java.io.IOException;

public class AbstractRDFTest {
    static protected BootstrapRepository repository;
    protected static ValueFactory vf = SimpleValueFactory.getInstance();
    protected static IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);

    public AbstractRDFTest() {}

    public static void bootUp() throws IOException {
        repository = new BootstrapRepository();
        vf = repository.getValueFactory();
        assert self.equals(repository.load(new File("src/test/resources/assets"), COMMONS.IQ_NS_TEST));
    }

    public void shutDown() throws RDFParseException, RepositoryException, IOException {
        System.out.println("test.rdf.shutdown @ " + getClass().getCanonicalName());
        repository.shutDown();
    }
}
