package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.rdf4j.io.IOCopier;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class WebpageIngestor extends AbstractConverter<ContentEntity<String>, ContentEntity<String>> {
protected WebpageIngestor() {}
public WebpageIngestor(Consumer<ContentEntity<String>> next) {
super(next);
}

protected ContentEntity<String> convert(ContentEntity<String> request) throws IOException {

URL page = new URL(request.getSelf().stringValue());
log.info("page.url: {}" , page.toExternalForm());
String content = IOCopier.toString(page.openConnection().getInputStream());
return new ContentEntity<>(Values.iri(page.toExternalForm()), content, "text/html");
}
}
