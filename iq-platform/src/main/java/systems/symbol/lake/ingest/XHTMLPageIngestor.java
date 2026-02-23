package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class XHTMLPageIngestor implements Consumer<FileObject> {
    protected static final Logger log = LoggerFactory.getLogger(XHTMLPageIngestor.class);
    protected Consumer<ContentEntity<String>> processor;
    protected XHTMLPageIngestor() {}
    public XHTMLPageIngestor(Consumer<ContentEntity<String>> processor) {
        this.processor = processor;
    }

    protected void parseXHTML(FileObject file) throws IOException {
        log.info("page.parse: " + file.getName().getURI());

        Document doc = Jsoup.parse(file.getContent().getInputStream(), "UTF-8", file.getName().getURI());
        Elements body = doc.select("body");
        parseSections(file, body);
    }

    protected void parseSections(FileObject file, Elements sections) {
        StringBuilder sb = new StringBuilder();
        for (Element element : sections) {
            sb.append(element.text());
        }
        if (processor !=null) {
            ContentEntity<String> entity = new ContentEntity<>(file.getPublicURIString(), sb.toString());
            processor.accept(entity);
        }
    }

    @Override
    public void accept(FileObject page) {
        try {
            parseXHTML(page);
        } catch (IOException e) {
            log.error("parse.failed: {}", page.getURI(), e);
        }
    }
}
