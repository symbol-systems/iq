package systems.symbol.camel.self;

import systems.symbol.assets.Asset;
import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AssetBase extends Base {
private static final Logger log = LoggerFactory.getLogger(AssetBase.class);

public AssetBase(ExecutionEnvironment engine, String uri) throws IOException {
super(engine, uri);
}

@Override
public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException {
// Execute the asset logic against the message exchange
if (asset == null) {
log.warn("Asset not loaded for URI: {}", uri);
exchange.getOut().setBody(exchange.getIn().getBody());
return;
}

try {
// Get the input body
Object input = exchange.getIn().getBody();

// Execute the asset with the input
// The asset should return a result that becomes the output
Object result = asset.execute(input, engine);

// Set the output body to the asset's result
exchange.getOut().setBody(result);

log.debug("Asset {} executed successfully", uri);
} catch (Exception ex) {
log.error("Asset {} execution failed: {}", uri, ex.getMessage(), ex);
// Pass through the original body on error
exchange.getOut().setBody(exchange.getIn().getBody());
exchange.setException(ex);
}
}
}
