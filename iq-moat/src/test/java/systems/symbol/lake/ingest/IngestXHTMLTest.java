package systems.symbol.lake.ingest;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.crawl.VFSCrawler;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

//    mvn test -Dtest="IngestXHTMLTest"
public class IngestXHTMLTest {
    @Test
    void testIngest() throws RepositoryException, IOException {
        File from = new File("./tested/tika/");
        boolean[]  done= {false};

        XHTMLChunkIngestor ingestHTML = new XHTMLChunkIngestor(t -> {
            System.out.println("tested: "+t.getIdentity()+" => "+ t.getContent().length());
            done[0] = !t.toString().isEmpty();
        });
        VFSCrawler crawler = new VFSCrawler(new FileContentConverter(ingestHTML));
        assert !done[0];
        crawler.crawl(from.toURI());
        assert done[0];
    }
}
