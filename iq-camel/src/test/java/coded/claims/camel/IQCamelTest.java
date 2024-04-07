package systems.symbol.camel;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;

public class IQCamelTest {
    public LocalAssetRepository repository;
    public ValueFactory vf;
    public IRI ctx;
    public TripleSource triples;
    public IRI iriHBS, iriGroovy, iriInfer, iriTestCase, iriSelect;

    @BeforeMethod
    public void setUp() throws IOException {
        repository = new LocalAssetRepository();
        ctx = repository.load(new File("src/test/resources/assets"), COMMONS.GG_TEST);
        vf = repository.getValueFactory();
        triples = repository.getTripleSource(true);
        iriTestCase = vf.createIRI(COMMONS.GG_TEST+"TestCase");
        iriInfer = vf.createIRI("https://test.symbol.systems/cases#queries/infer-labels.sparql");
        iriSelect = vf.createIRI("https://test.symbol.systems/cases#queries/inscheme.sparql");
        iriGroovy = vf.createIRI("https://test.symbol.systems/cases#scripts/hello.groovy");
        iriHBS = vf.createIRI("https://test.symbol.systems/cases#hbs/index.hbs");
    }
}
