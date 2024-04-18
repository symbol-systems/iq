package systems.symbol.rdf4j.sparql;

import systems.symbol.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.rdf4j.store.AbstractTripleTest;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.testng.annotations.Test;


public class ScriptCatalogTest extends AbstractTripleTest {

    @Test
    public void testGetSPARQLWithIRIFromConnection() {
        try (RepositoryConnection connection = assets.getConnection()) {
            ScriptCatalog library = new ScriptCatalog(new IQConnection(COMMONS.IQ_NS_TEST, connection));
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
            Literal sparql = ScriptCatalog.findScript(liveModel, iriSparqlQuery, vf.createIRI("urn:"+COMMONS.MIME_SPARQL), null);
            assert sparql !=null && !sparql.stringValue().isEmpty();
            assert sparql.stringValue().contains("SELECT ");
        }
    }
}
