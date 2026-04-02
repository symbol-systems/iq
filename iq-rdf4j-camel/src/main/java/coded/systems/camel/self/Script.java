package systems.symbol.camel.self;

import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import systems.symbol.iq.exec.Scripting;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.asset
 * @author Symbol Systems
 * Date  : 23/06/2014
 * Time  : 3:28 AM
 */
public class Script extends Base {

	public Script(ExecutionEnvironment engine, String uri) throws IOException {
		super(engine, uri);
	}

	@Override
	public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException {
		Map<String, Object> headers = exchange.getIn().getHeaders();
		exchange.getOut().setHeaders(headers);

		Scripting scripting = new Scripting();
		Future done = scripting.execute(asset, headers);
		exchange.getOut().setBody(done.get());
	}
}
