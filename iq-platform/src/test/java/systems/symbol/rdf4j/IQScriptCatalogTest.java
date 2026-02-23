package systems.symbol.rdf4j;

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
import systems.symbol.lake.BootstrapRepository;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.store.LiveModel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IQScriptCatalogTest {

    private static BootstrapRepository assets;
    private static ValueFactory vf;
    private static IRI ctx, iriSparqlQuery;

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        ctx = assets.load(new File("src/test/resources/assets"), IQ_NS.TEST);
        vf = assets.getValueFactory();
        iriSparqlQuery = vf.createIRI("iq:test:queries/all");
    }

    @AfterAll
    public static void tearDown() {
        if (assets != null) {
            assets.shutDown();
        }
    }

    @Test
    public void testIRIs() {
        assert ctx != null;
        assert ctx.stringValue().equals(IQ_NS.TEST);
    }

    @Test
    public void testGetSPARQLWithIRIFromConnection() {
        try (RepositoryConnection connection = assets.getConnection()) {
            IQScriptCatalog library = new IQScriptCatalog(new IQConnection(IQ_NS.TEST, connection));
            String sparql = library.getSPARQL(iriSparqlQuery);

            assertNotNull(sparql, "SPARQL query should not be null");
            assertTrue(!sparql.isEmpty(), "SPARQL query should not be empty");
            assertTrue(sparql.contains("SELECT "), "SPARQL query should contain 'SELECT'");
        } catch (Exception e) {
            throw new RuntimeException("Error while testing SPARQL retrieval from connection", e);
        }
    }

    @Test
    public void testGetSPARQLWithIRIFromModel() {
        try (RepositoryConnection connection = assets.getConnection()) {
            LiveModel liveModel = new LiveModel(connection);
            RDFDump.dump(liveModel, System.out, RDFFormat.TURTLE);

            Literal sparql = IQScripts.findScript(
                    liveModel,
                    iriSparqlQuery,
                    vf.createIRI("urn:" + COMMONS.MIME_SPARQL),
                    null);

            assertNotNull(sparql, "SPARQL script should not be null");
            assertTrue(!sparql.stringValue().isEmpty(), "SPARQL script should not be empty");
            assertTrue(sparql.stringValue().contains("SELECT "), "SPARQL script should contain 'SELECT'");
        } catch (Exception e) {
            throw new RuntimeException("Error while testing SPARQL retrieval from model", e);
        }
    }
}
