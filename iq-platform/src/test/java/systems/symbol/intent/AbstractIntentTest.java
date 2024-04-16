package systems.symbol.intent;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import systems.symbol.rdf4j.store.BootstrapRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

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
self = bootstrap.load(new File("src/test/resources/assets"), COMMONS.IQ_NS_TEST);
vf = bootstrap.getValueFactory();
//triples = assets.getTripleSource(true);
iriTestCase = vf.createIRI(COMMONS.IQ_NS_TEST +"TestCase");
iriSparqlQuery = vf.createIRI("urn:iq:test:queries/all");
iriHBSTemplate = vf.createIRI("urn:iq:test:hbs/index");

long count = bootstrap.getConnection().getStatements(null,null,null, self).stream().count();
System.out.println("test.activity.assets.loaded: "+count);
}

@AfterAll
public static void tearDown() {
}

}