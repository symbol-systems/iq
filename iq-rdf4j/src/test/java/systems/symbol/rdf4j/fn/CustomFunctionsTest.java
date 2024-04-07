package systems.symbol.rdf4j.fn;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

import java.io.File;
import java.io.IOException;

public class CustomFunctionsTest {
    LocalAssetRepository store = new LocalAssetRepository(new File("src/test/resources/"),COMMONS.IQ_NS_TEST);
    TripleSource tripleSource = store.getTripleSource(true);
    ValueFactory vf = tripleSource.getValueFactory();
    IRI ctx = vf.createIRI(COMMONS.IQ_NS_TEST);

    // ValueFactory vf = new ValidatingValueFactory();
    public CustomFunctionsTest() throws IOException {
    }
}