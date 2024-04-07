package systems.symbol.camel.self;

import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.asset
 * @author Symbol Systems
 * Date  : 23/06/2014
 * Time  : 4:53 PM
 */
public class Deploy extends Base {
	static protected final Logger log = LoggerFactory.getLogger(Deploy.class);

	public Deploy(String uri) throws IOException, FactException {
		super(engine, uri);

	}

	@Handler
	public void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException {
		RepositoryConnection connection = engine.getRepository().getConnection();
		BulkAssetLoader deployer = new BulkAssetLoader(engine.getIdentity(), connection);
		deployer.setDeployScripts(false);
		deployer.setDeployRDF(true);

		Map<String, Object> headers = exchange.getIn().getHeaders();
		File file = exchange.getIn().getBody(File.class);
		log.info("Exec deploy: "+uri+" -> "+file);

		if (file!=null) {
			String from = exchange.getFromEndpoint().getEndpointUri();
			log.info("Deploy File: "+from+" -> "+file.getAbsolutePath());
			file = new File(file.getAbsolutePath());
			deployer.deploy(file.getParentFile(), file);
		}
		if (uri!=null && !uri.isEmpty()) {
			log.info("Deploy URL: "+uri);
			deployer.deploy(new URL(uri));
		}
		connection.close();

		exchange.getOut().setBody(file);
		exchange.getOut().setHeaders(headers);
	}

}
