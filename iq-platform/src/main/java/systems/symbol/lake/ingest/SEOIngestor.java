package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class SEOIngestor implements Consumer<FileObject> {
    protected static final Logger log = LoggerFactory.getLogger(SEOIngestor.class);
    protected Set<URI> seen = new HashSet<>();
    protected Consumer<ContentEntity<String>> next;
    protected SEOIngestor() {}
    public SEOIngestor(Consumer<ContentEntity<String>> next) throws FileSystemException {
        this.next = next;
    }
    @Override
    public void accept(FileObject site) {
        if (seen.contains(site.getURI())) {
            return;
        }
        try {
            ingest(site);
        } catch (IOException e) {
            log.error("ingest.failed", e);
            throw new RuntimeException(e);
        }
    }

    protected void ingest(FileObject file) throws IOException {
        boolean visited = this.seen.contains(file.getURI());
        log.info("seo.visit: {} -> {}", !visited, file.getURI());
        if (visited) return;
        this.seen.add(file.getURI());
        Document doc = Jsoup.parse(file.getContent().getInputStream(), "UTF-8", file.getName().getURI());
        ContentEntity<String> content = new ContentEntity<String>(file.getURI().toString(), doc.html());
        if (next !=null) next.accept(content);
    }

}
