package systems.symbol.camel.self;

import systems.symbol.assets.Asset;
import systems.symbol.oops.AssetNotSupported;
import systems.symbol.oops.ConfigException;
import systems.symbol.oops.FactException;
import systems.symbol.oops.IQException;
import systems.symbol.runtime.ExecutionEnvironment;
import systems.symbol.Identifiable;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.asset
 * @author Symbol Systems
 * Date  : 23/06/2014
 * Time  : 3:28 AM
 */
abstract public class Base implements Identifiable {
	static protected final Logger log = LoggerFactory.getLogger(Base.class);
	protected String uri;
	protected Asset asset;

	protected Base() throws IOException {
	}

	public Base(String uri) throws IOException {
		init(engine,uri);
	}

	public void init(String uri) throws IOException {
		this.engine = engine;
		this.uri=uri;
		if (uri!=null && !uri.isEmpty()) {
			try {
				asset = engine.getAssetRegister().getAsset(uri,null);
			} catch(Exception e) {
				throw new IOException(e.getMessage(),e);
			}
			log.info("Asset for "+getClass().getSimpleName()+" -> "+(asset==null?"not found: ":asset.getContentType())+" -> "+uri+"\n"+asset);
		}
	}

	public ExecutionEnvironment getEngine() {
		return engine;
	}

	public Asset getAsset(String uri, String type) throws IOException {
		if (asset==null) return engine.getAssetRegister().getAsset( uri, type );
		else return asset;
	}

	@Handler
	public abstract  void execute(Exchange exchange) throws RepositoryException, ExecutionException, IQException, InterruptedException, IOException, AssetNotSupported, FactException, ConfigException, QueryEvaluationException, MalformedQueryException;

	public String getIdentity() {
		return uri;
	}

}
