package systems.symbol.camel.routing;


import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class IQRouters extends RouteBuilder {
protected static final Logger log = LoggerFactory.getLogger(IQRouters.class);

@Autowired
protected IQ iq;
protected final ArrayList<RoutePolicy> policies = new ArrayList<>();

public IQRouters() {
}

public IQRouters(IQ iq) {
this.iq = iq;
}

@Override
public void configure() throws Exception {
log.info("iq.router.base: "+iq);
policies.add( new IQRoutePolicy(iq));
log.info("iq.router.camel.name: "+getContext().getName());

// trigger event every 60 seconds
Map<String,Object > props = new HashMap<>();
props.put("period", "60000");

String timerRoute = toRouteURL("timer:every-minute", props);
RouteDefinition timer = from(timerRoute);
timer.transform().constant("Hello IQ: "+ timerRoute).log("${body}");
policy(timer);

log.info("iq.router.camel.components: "+getContext().getComponentNames());
//RouteDefinition test = from("servlet:rdf/infer").
//setProperty("matchOnUriPrefix", constant(true)).log("servlet-test").to("direct:rdf4j-infer");
//policy(test);
//
//RouteDefinition rdf4j_infer = from("direct:rdf4j-infer")
//.to("rdf4j:default/queries/infer.sparql").marshal().json().log("rdf4j-infer");
//policy(rdf4j_infer);

//policy(from("servlet:nlp/text").to("nlp:text").marshal().json().log("rdf4j-nlp"));
//policy(from("servlet:nlp/import").to("nlp:text").to("rdf4j:default:import").marshal().json().log("rdf4j-nlp"));
}


public static String toRouteURL(String iri, Map<String,Object> params) throws URISyntaxException {
return URISupport.createURIWithQuery( new URI(iri), URISupport.createQueryString(params, true)).toString();
}

public RouteDefinition policy(RouteDefinition routeDefinition) {
routeDefinition.setRoutePolicies(policies);
routeDefinition.setGroup(iq.getIdentity().stringValue());
return routeDefinition;
}
}
