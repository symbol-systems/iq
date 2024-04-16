package systems.symbol.rdf4j.store;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

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
ctx = assets.load(new File("src/test/resources/assets"), COMMONS.IQ_NS_TEST);
vf = assets.getValueFactory();
//triples = assets.getTripleSource(true);
iriTestCase = vf.createIRI(COMMONS.IQ_NS_TEST +"TestCase");
iriSparqlQuery = vf.createIRI("urn:iq:test:queries/all.sparql");
iriHBSTemplate = vf.createIRI("urn:iq:test:hbs/index.hbs");
try (RepositoryConnection connection = assets.getConnection()) {
long count = connection.getStatements(null,null,null,ctx).stream().count();
System.out.println("test.assets.loaded: "+count);
}
}

@AfterMethod
public void tearDown() {
}

}