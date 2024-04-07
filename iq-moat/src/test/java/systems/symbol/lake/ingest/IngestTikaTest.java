package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFSCrawler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


//    mvn test -Dtest="IngestTikaTest"

public class IngestTikaTest {
    File from = new File("./src/test/resources/docs/");
    File to = new File("./tested/tika/");


    @Test
    void testIngest() throws RepositoryException, IOException {
        boolean done[] = {false};
        TikaDocumentIngestor<Object> tika = new TikaDocumentIngestor<>(entity -> {
            done[0] = entity.getContent().toString().indexOf("dc:title") > 0;
        });
        VFSCrawler crawler = new VFSCrawler(tika);
        IRI crawled = crawler.crawl(from.toURI());
        assert crawled!=null;
        assert done[0];
    }
}
