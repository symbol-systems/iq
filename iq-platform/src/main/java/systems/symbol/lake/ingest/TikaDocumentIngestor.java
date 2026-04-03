package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.exception.TikaException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.eclipse.rdf4j.model.util.Values;

import java.io.*;
import java.util.function.Consumer;

public class TikaDocumentIngestor extends AbstractConverter<FileObject, ContentEntity<String>> {
private final Parser nativeParser = new AutoDetectParser();

public TikaDocumentIngestor() {
}

public TikaDocumentIngestor(Consumer<ContentEntity<String>> next) {
super(next);
}

@Override
public ContentEntity<String> convert(FileObject file) throws IOException {
log.debug("tika.from: {}", file.getName().getURI());
ContentHandler toXML = new ToXMLContentHandler();
Metadata metadata = new Metadata();
ParseContext context = new ParseContext();

try (InputStream in = file.getContent().getInputStream()) {
nativeParser.parse(in, toXML, metadata, context);
} catch (TikaException | SAXException e) {
throw new IOException("Tika parse failure", e);
}

ContentEntity<String> to = new ContentEntity<>(Values.iri(file.getPublicURIString()), toXML.toString(), "application/xhtml+xml");
log.debug("tika.to: " + to.getSelf());
return to;
}
}
