package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.BootstrapRepository;

import java.io.File;
import java.io.IOException;

public class CustomFunctionsTest {
    BootstrapRepository store = new BootstrapRepository(new File("src/test/resources/"), IQ_NS.TEST);
    TripleSource tripleSource = store.getTripleSource(true);
    ValueFactory vf = tripleSource.getValueFactory();
    IRI ctx = vf.createIRI(IQ_NS.TEST);

    // ValueFactory vf = new ValidatingValueFactory();
    public CustomFunctionsTest() throws IOException {
    }
}