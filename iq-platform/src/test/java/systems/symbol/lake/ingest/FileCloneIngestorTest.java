package systems.symbol.lake.ingest;

import systems.symbol.vfs.MyVFS;
import systems.symbol.lake.crawl.VFSCrawler;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

class FileCloneIngestorTest {

    @Test
    void copyFileToFolder() throws FileSystemException, URISyntaxException {
        MyVFS vfs = new MyVFS();
        String from = "./src/test/resources/docs/Example.pdf";
        FileObject to = vfs.resolveFile("./tested/");

        FileCloneIngestor ingestor = new FileCloneIngestor(to, null);
        VFSCrawler crawler = new VFSCrawler(ingestor);
        crawler.crawl(new URI(from));
        vfs.close();
    }
}