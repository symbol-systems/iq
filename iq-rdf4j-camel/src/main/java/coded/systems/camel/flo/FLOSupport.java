package systems.symbol.camel.flo;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel
 * @author Symbol Systems
 * Date  : 19/06/2014
 * Time  : 8:22 AM
 */
public class FLOSupport {
	private static final Logger log = LoggerFactory.getLogger(FLOSupport.class);
	protected CamelContext context;
	protected int count = 0;

	public FLOSupport() throws Exception {
		this.context = new DefaultCamelContext();
		this.context.start();
	}

	public FLOSupport(CamelContext context) {
		this.context=context;
	}

	public int plan() throws Exception {
		// do nothing
		return 0;
	}

	public RouteBuilder plan(final String _from, final String _to) throws Exception {
		RouteBuilder routing = new org.apache.camel.builder.RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from(_from).to(_to).end();
			}
		};
		context.addRoutes(routing);
		return routing;
	}

	public RouteBuilder plan(final String _from, final Collection<String> _tos) throws Exception {
		RouteBuilder routing = new org.apache.camel.builder.RouteBuilder() {
			@Override
			public void configure() throws Exception {
				int i=0;
				RouteDefinition route = from(_from);
				for(String to : _tos) {
					route  = route.to(to);
				}
				route.end();
			}
		};
		context.addRoutes(routing);
		return routing;
	}

//	public Object trigger(String from, Object body) {
//		return trigger(from, body, new HashMap(), Object.class);
//	}

	public Object trigger(String from, Object body, Map header, Class type) {
		if (canDereference(from)||from.startsWith("urn:")) from = "direct://"+from;
		try {
			from = URISupport.normalizeUri(from);
			if (context.hasEndpoint(from)==null) {
				log.debug("Missing Endpoint: " + from );
				return null;
			}
			log.debug("Trigger: " + from );
			ProducerTemplate producer = context.createProducerTemplate();
			Object result = producer.requestBodyAndHeaders(from, body, header, type);
			return result;
		} catch(Exception e) {
			log.error("Faulty Trigger: " + from + " --> " + e.getMessage(), e);
			return body;
		}
	}

	public void start() throws Exception {
		context.start();
	}

	public void restart() throws Exception {
		context.suspend();
		plan();
		context.resume();
	}

	public static boolean canDereference(String to) {
		return to.startsWith("http:") || to.startsWith("https:") || to.startsWith("file:");
	}


}
