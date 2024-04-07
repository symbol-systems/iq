package systems.symbol.lake.ingest;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import systems.symbol.platform.Consumers;
import systems.symbol.lake.crawl.VFSCrawler;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

//mvn test -Dtest="IngestChainTest"

public class ChainTest extends AbstractRDFTest {

@BeforeEach
void setUp() throws RDFParseException, RepositoryException, IOException {
bootUp();
}

@AfterEach
void tearDown() throws RDFParseException, RepositoryException, IOException {
shutDown();
}

@Test
void testIngestChain() throws FileSystemException, ParserConfigurationException {
File assetsHome = new File("./src/test/resources/assets").getAbsoluteFile();

try (RepositoryConnection connection = repository.getConnection()) {
Consumer<FileObject> rdf = new RDFFileIngestor(connection, self);
Consumer<FileObject> txt = new TextAsRDFIngestor(connection);

Consumer chain = new Consumers(rdf).add(txt);
VFSCrawler crawler = new VFSCrawler(chain);
crawler.crawl(assetsHome.toURI());
}
}

}
