package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import io.quarkus.tika.TikaParser;
import org.apache.commons.vfs2.FileObject;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.eclipse.rdf4j.model.util.Values;

import java.io.*;
import java.util.function.Consumer;

public class TikaDocumentIngestor extends AbstractConverter<FileObject, ContentEntity<String>> {
    Parser nativeParser = new AutoDetectParser();
    private final TikaParser tika;

    public TikaDocumentIngestor() {
        this.tika = new TikaParser(nativeParser, true);
    }

    public TikaDocumentIngestor(Consumer<ContentEntity<String>> next) {
        super(next);
        this.tika = new TikaParser(nativeParser, true);
    }

    @Override
    public ContentEntity<String> convert(FileObject file) throws IOException {
        log.debug("tika.from: {}", file.getName().getURI());
        ToXMLContentHandler toXML = new ToXMLContentHandler();
        InputStream in = file.getContent().getInputStream();
        tika.parse(in, toXML);
        in.close();
        ContentEntity<String> to = new ContentEntity<>(Values.iri(file.getPublicURIString()), toXML.toString(), "application/xhtml+xml");
        log.debug("tika.to: " + to.getSelf());
        return to;
    }
}
