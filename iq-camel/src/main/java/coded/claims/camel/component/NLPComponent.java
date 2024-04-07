package systems.symbol.camel.component;

import systems.symbol.camel.processor.nlp.NLPProcessor;
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
public class NLPComponent extends DefaultComponent {
	static protected final Logger log = LoggerFactory.getLogger(RDF4JComponent.class);

	public NLPComponent() {
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		return new ProcessorEndpoint(uri, this, new NLPProcessor());
	}
}
