package systems.symbol.lake.ingest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import systems.symbol.lake.BootstrapRepository;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

public class AbstractRDFTest {
    static protected BootstrapRepository repository;
    protected static ValueFactory vf = SimpleValueFactory.getInstance();
    protected static IRI self = vf.createIRI(IQ_NS.TEST);

    public AbstractRDFTest() {
    }

    public static void bootUp() throws IOException {
        repository = new BootstrapRepository();
        vf = repository.getValueFactory();
        assert self.equals(repository.load(new File("src/test/resources/assets"), IQ_NS.TEST));
    }

    public void shutDown() throws RDFParseException, RepositoryException, IOException {
        System.out.println("test.rdf.shutdown @ " + getClass().getCanonicalName());
        repository.shutDown();
    }
}
