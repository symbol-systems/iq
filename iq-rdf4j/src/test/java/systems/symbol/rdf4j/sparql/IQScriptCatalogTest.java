package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.testng.annotations.Test;
import systems.symbol.COMMONS;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.AbstractTripleTest;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.store.LiveModel;


public class IQScriptCatalogTest extends AbstractTripleTest {

@Test
public void testGetSPARQLWithIRIFromConnection() {
try (RepositoryConnection connection = assets.getConnection()) {
IQScriptCatalog library = new IQScriptCatalog(new IQConnection(IQ_NS.TEST, connection));
String sparql = library.getSPARQL(iriSparqlQuery);
assert sparql !=null && !sparql.isEmpty();
assert sparql.contains("SELECT ");
}
}

@Test
public void testGetSPARQLWithIRIFromModel() throws Exception {
try (RepositoryConnection connection = assets.getConnection()) {
LiveModel liveModel = new LiveModel(connection);
RDFDump.dump(liveModel, System.out, RDFFormat.TURTLE);
Literal sparql = IQScripts.findScript(liveModel, iriSparqlQuery, vf.createIRI("urn:"+COMMONS.MIME_SPARQL), null);
assert sparql !=null && !sparql.stringValue().isEmpty();
assert sparql.stringValue().contains("SELECT ");
}
}
}
