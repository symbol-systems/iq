package systems.symbol.lake.crawl;

import systems.symbol.lake.ingest.FileContentConverter;
import systems.symbol.tools.Online;

import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

class VFSCrawlerTest {
    String ARXIV = "https://export.arxiv.org/list/cs.AI/recent";

    @Test
    void crawlArxivWebPage() throws FileSystemException, URISyntaxException {
        if (!Online.isOnline("https://export.arxiv.org"))
            return;
        boolean[] done = { true };
        VFSCrawler crawler = new VFSCrawler(new FileContentConverter(entity -> {
            done[0] = entity.getContent().contains("html");
        }));
        try {
            System.out.println("crawling: " + ARXIV);
            IRI crawled = crawler.crawl(new URI(ARXIV));
            assert crawled != null;
            assert done[0];
        } catch (Exception e) {
            System.err.println("crawling.failed: " + ARXIV);
            e.printStackTrace();
        }
    }
}
