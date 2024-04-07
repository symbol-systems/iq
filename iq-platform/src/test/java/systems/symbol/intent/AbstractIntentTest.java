package systems.symbol.intent;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;

public class AbstractIntentTest {
    public static LocalAssetRepository assets;
    public static ValueFactory vf;
    public static IRI self, iriSparqlQuery, iriTestCase;
    public static IRI iriHBSTemplate;

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new LocalAssetRepository();
        self = assets.load(new File("src/test/resources/assets"), COMMONS.IQ_NS_TEST);
        vf = assets.getValueFactory();
//        triples = assets.getTripleSource(true);
        iriTestCase = vf.createIRI(COMMONS.IQ_NS_TEST +"TestCase");
        iriSparqlQuery = vf.createIRI("https://test.symbol.systems/cases#queries/all.sparql");
        iriHBSTemplate = vf.createIRI("https://test.symbol.systems/cases#hbs/index.hbs");

        long count = assets.getConnection().getStatements(null,null,null, self).stream().count();
System.out.println("test.activity.assets.loaded: "+count);
    }

    @AfterAll
    public static void tearDown() {
    }

}