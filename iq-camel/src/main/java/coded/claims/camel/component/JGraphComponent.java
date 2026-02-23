package systems.symbol.camel.component;

import systems.symbol.camel.processor.jgraph.JGraphProcessor;
import systems.symbol.util.Steps;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.ProcessorEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class JGraphComponent extends DefaultComponent {
	static protected final Logger log = LoggerFactory.getLogger(RDF4JComponent.class);

	public JGraphComponent() {
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		Steps steps = new Steps(uri);
		String algo = steps.step();
		log.info("camel.iq.jgraph.algo: "+algo);
		JGraphProcessor processor = new JGraphProcessor(algo);
		return new ProcessorEndpoint(uri, this, processor);
	}
}
