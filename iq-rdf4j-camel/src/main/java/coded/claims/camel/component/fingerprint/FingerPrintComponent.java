package systems.symbol.camel.component.fingerprint;

import org.apache.camel.Endpoint;
import org.apache.camel.component.bean.BeanEndpoint;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.component.bean.ClassComponent;
import org.apache.camel.util.IntrospectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component
 * @author Symbol Systems
 * Date  : 26/06/2014
 * Time  : 11:20 PM
 */
public class FingerPrintComponent extends ClassComponent {
	protected final Logger log = LoggerFactory.getLogger(FingerPrintComponent.class);
	protected Object bean;

	public FingerPrintComponent(Object bean) {
		this.bean = bean;
	}

	public Object getBean() {
		return bean;
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		Map<String, Object> params = IntrospectionSupport.extractProperties(parameters, "iq.");
		return new BeanEndpoint(uri,this, new BeanProcessor(new FingerprintHandler(), getCamelContext() ));
	}
}
