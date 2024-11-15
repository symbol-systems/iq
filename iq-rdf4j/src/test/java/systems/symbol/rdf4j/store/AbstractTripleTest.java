package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

@TestInstance(Lifecycle.PER_CLASS)
public class AbstractTripleTest {
public BootstrapRepository assets;
public ValueFactory vf;
public IRI ctx, iriSparqlQuery, iriTestCase;
// public TripleSource triples;
public IRI iriHBSTemplate;

@BeforeAll
public void setUp() throws IOException {
assets = new BootstrapRepository();
ctx = assets.load(new File("src/test/resources/assets"), IQ_NS.TEST);
vf = assets.getValueFactory();

iriTestCase = vf.createIRI(IQ_NS.TEST + "TestCase");
iriSparqlQuery = vf.createIRI("iq:test:queries/all");
iriHBSTemplate = vf.createIRI("iq:test:hbs/index");
}

@AfterAll
public void tearDown() {
assets.shutDown();
}

}