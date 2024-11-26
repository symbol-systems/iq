package systems.symbol.lake.ingest;

import okhttp3.Response;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.tools.APIException;
import systems.symbol.tools.RestAPI;
import systems.symbol.lake.ContentEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public class URLIngestor extends AbstractConverter<ContentEntity<String>, ContentEntity<String>> {
    protected URLIngestor() {
    }

    public URLIngestor(Consumer<ContentEntity<String>> next) {
        super(next);
    }

    protected ContentEntity<String> convert(ContentEntity<String> request) throws IOException {
        String url = request.getSelf().stringValue();
        RestAPI api = new RestAPI(url);
        log.info("page.url: {}", url);
        try {
            Response response = api.get(null);
            String type = toMediaType(response.header("content-type"));
            String content = Objects.requireNonNull(response.body()).string();
            log.info("page.content: {} -> {} --> {}", url, type, content.length());
            return new ContentEntity<>(Values.iri(url), content, type);

        } catch (APIException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public String toMediaType(String contentType) {
        if (contentType == null)
            return null;
        String[] parts = contentType.toLowerCase().split(";");
        if (parts.length < 1)
            return null;
        return parts[0].trim();
    }
}
