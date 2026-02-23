package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import systems.symbol.lake.BootstrapRepository;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

public class AbstractIntentTest {
    public static BootstrapRepository bootstrap;
    public static ValueFactory vf;
    public static IRI self, iriSparqlQuery, iriTestCase;
    public static IRI iriHBSTemplate;

    @BeforeAll
    public static void setUp() throws IOException {
        bootstrap = new BootstrapRepository();
        self = bootstrap.load(new File("src/test/resources/assets"), IQ_NS.TEST);
        vf = bootstrap.getValueFactory();
        // triples = assets.getTripleSource(true);
        iriTestCase = vf.createIRI(IQ_NS.TEST + "TestCase");
        iriSparqlQuery = vf.createIRI("iq:test:queries/all");
        iriHBSTemplate = vf.createIRI("iq:test:hbs/index");
    }

    @AfterAll
    public static void tearDown() {
    }

}