package systems.symbol.lake.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//    mvn test -Dtest="IngestTextAsRDFTest"

public class IngestTextAsRDFTest extends AbstractRDFTest {

    @BeforeEach
    void setUp() throws RDFParseException, RepositoryException, IOException {
        bootUp();
    }

    @AfterEach
    void tearDown() throws RDFParseException, RepositoryException, IOException {
        shutDown();
    }

    @Test
    void testIngest() throws RepositoryException, IOException {
        File to = new File("./src/test/resources/assets/").getAbsoluteFile();

        try (RepositoryConnection connection = repository.getConnection()) {
            TextAsRDFIngestor ingestRDF = new TextAsRDFIngestor(connection);
            VFSCrawler crawler = new VFSCrawler(ingestRDF);

            System.out.println("test.rdf.txt.iri: " + to.toURI());

            crawler.crawl(to.toURI());
            connection.close();
        }

        System.out.println("test.rdf.txt.loaded: " + new Date());

        try (RepositoryConnection conn = repository.getConnection()) {
            assertFalse(conn.isEmpty());
            RepositoryResult<Statement> statements = conn.getStatements(null, IQScriptCatalog.HAS_CONTENT, null, true);
            assertTrue(statements.hasNext());
            for (Statement statement : statements) {
                assertEquals(IQScriptCatalog.HAS_CONTENT, statement.getPredicate());
            }
        }
    }
}
