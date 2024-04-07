package systems.symbol.camel.component;

import systems.symbol.camel.processor.rdf4j.GraphQueryProcessor;
import systems.symbol.camel.processor.rdf4j.ImportModelProcessor;
import systems.symbol.rdf4j.store.Workspace;
import systems.symbol.util.Steps;
import org.apache.camel.*;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultEndpoint;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel
 * @author Symbol Systems
 * Date  : 22/06/2014
 * Time  : 11:51 PM
 */
@Component
public class RDF4JComponent extends DefaultComponent {
	static protected final Logger log = LoggerFactory.getLogger(RDF4JComponent.class);

	@Autowired
	protected Workspace workspace;

	public RDF4JComponent() {
		log.info("iq.camel.rdf.init");
	}

	public Workspace getRepositories() {
		return workspace;
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		Boolean isInferred = getAndRemoveParameter(parameters, "inferred", Boolean.class, true);
		Integer maxQueryTime = getAndRemoveParameter(parameters, "max-execution-time", Integer.class, 0);

		Steps steps = new Steps(uri);
		String repo = steps.step();
		Repository repository = workspace.getRepository(repo);
		log.info("iq.camel.rdf.repository: "+uri+" -> "+repo+" -> "+repository);
		if (repository==null) throw new RepositoryException("Missing repository: "+repo);

		// a path means we have a query, otherwise just import the message model
		if (steps.size()>0) {
			return new RDF4JEndpoint(this, uri, new GraphQueryProcessor(repository, isInferred, maxQueryTime, steps.toString()));
		} else {
			return new RDF4JEndpoint(this, uri, new ImportModelProcessor(repository));
		}
	}
}

class RDF4JEndpoint extends DefaultEndpoint {
	RDF4JComponent component;
	Processor processor = null;
	String uri;

	public RDF4JEndpoint(RDF4JComponent component, String uri, Processor processor) {
		assert component!=null;
		assert processor!=null;
		this.component=component;
		this.processor=processor;
		this.uri=uri;
	}

	@Override
	public Producer createProducer() throws Exception {
		DefaultEndpoint self = this;
		return new Producer() {
			@Override
			public Endpoint getEndpoint() {
				return self;
			}

			@Override
			public boolean isSingleton() {
				return false;
			}

			@Override
			public void process(Exchange exchange) throws Exception {
				if (processor!=null) processor.process(exchange);
			}

			@Override
			public void start() {
				// NOP
			}

			@Override
			public void stop() {
				// NOP
			}
		};
	}

	protected String createEndpointUri() {
		return uri;
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		return null;
	}
}
