package systems.symbol.lake.ingest;

import okhttp3.Response;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.lake.ContentEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public class URLIngestor extends AbstractConverter<ContentEntity<String>, ContentEntity<String>> {
protected URLIngestor() {}
public URLIngestor(Consumer<ContentEntity<String>> next) {
super(next);
}

protected ContentEntity<String> convert(ContentEntity<String> request) throws IOException {
String url = request.getSelf().stringValue();
RestAPI api = new RestAPI(url);
log.info("page.url: {}" , url);
try {
Response response = api.get(null);
String content = Objects.requireNonNull(response.body()).string();
return new ContentEntity<>(Values.iri(url), content);
} catch (APIException e) {
throw new IOException(e.getMessage(), e);
}
}
}
