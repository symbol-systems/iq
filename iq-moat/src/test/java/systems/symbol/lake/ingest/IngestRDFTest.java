package systems.symbol.lake.ingest;

import java.io.File;
import java.io.IOException;

import systems.symbol.lake.crawl.VFSCrawler;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//    mvn test -Dtest="IngestRDFTest"

public class IngestRDFTest extends AbstractRDFTest {

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
        File from = new File("./src/test/resources/rdf/");

        try (RepositoryConnection conn = repository.getConnection()) {
            RDFFileIngestor ingestRDF = new RDFFileIngestor(conn, self);
            VFSCrawler crawler = new VFSCrawler(ingestRDF);
            crawler.crawl(from.toURI());
        }
    }
}
