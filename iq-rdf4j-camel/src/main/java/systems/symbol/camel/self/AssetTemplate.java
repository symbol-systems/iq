package systems.symbol.camel.self;

import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.render.PicoTemplate;
import systems.symbol.runtime.ExecutionEnvironment;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class AssetTemplate extends Base {
private static final Logger log = LoggerFactory.getLogger(AssetTemplate.class);

public AssetTemplate(ExecutionEnvironment engine, String uri) throws IOException {
super(engine, uri);
}

@Override
public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException {
// Apply template rendering to the message
if (asset == null) {
log.warn("Template asset not loaded for URI: {}", uri);
exchange.getOut().setBody(exchange.getIn().getBody());
return;
}

try {
// Get the input message headers and body for template context
Map<String, Object> context = exchange.getIn().getHeaders();
Object body = exchange.getIn().getBody();

// Create a template renderer from the asset content
PicoTemplate template = new PicoTemplate(asset.toString());

// Render the template with the message context
String result = template.render(context);

// Set the output body to the rendered template
exchange.getOut().setBody(result);
exchange.getOut().setHeaders(context);

log.debug("Template {} rendered successfully", uri);
} catch (Exception ex) {
log.error("Template {} rendering failed: {}", uri, ex.getMessage(), ex);
// Pass through the original body on error
exchange.getOut().setBody(exchange.getIn().getBody());
exchange.setException(ex);
}
}
}
