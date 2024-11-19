package systems.symbol.rdf4j.sparql;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.COMMONS;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.store.LiveModel;

public class IQScriptCatalogTest {
public BootstrapRepository assets;
public ValueFactory vf;
public IRI ctx, iriSparqlQuery, iriTestCase;
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

@Test
public void testGetSPARQLWithIRIFromConnection() {
try (RepositoryConnection connection = assets.getConnection()) {
IQScriptCatalog library = new IQScriptCatalog(new IQConnection(IQ_NS.TEST, connection));
String sparql = library.getSPARQL(iriSparqlQuery);
assert sparql != null && !sparql.isEmpty();
assert sparql.contains("SELECT ");
}
}

@Test
public void testGetSPARQLWithIRIFromModel() throws Exception {
try (RepositoryConnection connection = assets.getConnection()) {
LiveModel liveModel = new LiveModel(connection);
RDFDump.dump(liveModel, System.out, RDFFormat.TURTLE);
Literal sparql = IQScripts.findScript(liveModel, iriSparqlQuery, vf.createIRI("urn:" + COMMONS.MIME_SPARQL),
null);
assert sparql != null && !sparql.stringValue().isEmpty();
assert sparql.stringValue().contains("SELECT ");
}
}
}
