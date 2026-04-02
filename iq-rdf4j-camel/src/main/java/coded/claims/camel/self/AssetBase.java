package systems.symbol.camel.self;

import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AssetBase extends Base {
public AssetBase(ExecutionEnvironment engine, String uri) throws IOException {
super(engine, uri);
}

@Override
public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException {
// no-op stub
exchange.getOut().setBody(exchange.getIn().getBody());
}
}
