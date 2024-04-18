package systems.symbol.rdf4j.fn;

import systems.symbol.COMMONS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

import java.io.File;
import java.io.IOException;

public class CustomFunctionsTest {
    BootstrapRepository store = new BootstrapRepository(new File("src/test/resources/"),COMMONS.IQ_NS_TEST);
    TripleSource tripleSource = store.getTripleSource(true);
    ValueFactory vf = tripleSource.getValueFactory();
    IRI ctx = vf.createIRI(COMMONS.IQ_NS_TEST);

    // ValueFactory vf = new ValidatingValueFactory();
    public CustomFunctionsTest() throws IOException {
    }
}