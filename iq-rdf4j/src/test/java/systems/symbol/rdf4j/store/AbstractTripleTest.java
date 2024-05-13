package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

public class AbstractTripleTest {
public BootstrapRepository assets;
public ValueFactory vf;
public IRI ctx, iriSparqlQuery, iriTestCase;
//public TripleSource triples;
public IRI iriHBSTemplate;

@BeforeMethod
public void setUp() throws IOException {
assets = new BootstrapRepository();
ctx = assets.load(new File("src/test/resources/assets"), IQ_NS.TEST);
vf = assets.getValueFactory();

iriTestCase = vf.createIRI(IQ_NS.TEST +"TestCase");
iriSparqlQuery = vf.createIRI("urn:iq:test:queries/all");
iriHBSTemplate = vf.createIRI("urn:iq:test:hbs/index");
}

@AfterMethod
public void tearDown() {
assets.shutDown();
}

}