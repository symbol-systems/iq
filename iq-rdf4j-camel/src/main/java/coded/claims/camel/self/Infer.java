package systems.symbol.camel.self;

import systems.symbol.assets.Asset;
import systems.symbol.assets.AssetHelper;
import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import systems.symbol.rdf4j.io.SPARQLRules;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.asset
 * @author Symbol Systems
 * Date  : 23/06/2014
 * Time  : 3:28 AM
 */
public class Infer extends Base {

	public Infer(String uri) throws IOException {
		super(engine,uri);
	}

	@Override
	public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, QueryEvaluationException, MalformedQueryException {
		Map<String, Object> headers = exchange.getIn().getHeaders();
		exchange.getOut().setHeaders(headers);
		exchange.getOut().setAttachments(exchange.getIn().getAttachments());

		RepositoryConnection connection = getEngine().getRepository().getConnection();
		SPARQLRules SPARQLRules = new SPARQLRules(connection, getIdentity());
		Asset newAsset = AssetHelper.getAsset(asset, headers);
		int copied = SPARQLRules.apply(newAsset.toString());

		exchange.getOut().setBody(exchange.getIn().getBody());
		connection.close();
	}


}
