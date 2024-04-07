package systems.symbol.camel.self;

import systems.symbol.crud.Model;
import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import systems.symbol.render.PicoTemplate;
import systems.symbol.rdf4j.util.SesameHelper;
import org.apache.camel.Exchange;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.asset
 * @author Symbol Systems
 * Date  : 23/06/2014
 * Time  : 3:28 AM
 */
public class SPARQL extends Base {

	public SPARQL(String uri) throws IOException {
		super(engine,uri);
	}

	@Override
	public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException {
		Map<String, Object> headers = exchange.getIn().getHeaders();
		exchange.getOut().setHeaders(headers);
		exchange.getOut().setAttachments(exchange.getIn().getAttachments());

		PicoTemplate picoTemplate = new PicoTemplate(asset.toString());
		RepositoryConnection connection = getEngine().getRepository().getConnection();

		Collection<Model> result = SesameHelper.toModels(connection, picoTemplate.translate(headers), null, "this");
		exchange.getOut().setBody(result);
		connection.close();
	}
}
