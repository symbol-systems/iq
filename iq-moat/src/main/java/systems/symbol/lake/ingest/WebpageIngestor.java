package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.rdf4j.io.IOCopier;
import systems.symbol.rdf4j.iq.IQ;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

public class WebpageIngestor extends AbstractConverter<ContentEntity<String>, ContentEntity<String>> {
protected WebpageIngestor() {}
public WebpageIngestor(Consumer<ContentEntity<String>> next) {
super(next);
}

protected ContentEntity<String> convert(ContentEntity<String> request) throws IOException {
URL page = new URL(request.getIdentity().stringValue());
log.debug("url.convert: {}" , page.toExternalForm());
String content = IOCopier.toString(page.openConnection().getInputStream());
return new ContentEntity<>(Values.iri(page.toExternalForm()), content);
}

}
