package systems.symbol.camel;

import systems.symbol.camel.routing.IQRoutePolicy;
import systems.symbol.camel.routing.IQRouters;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.iq.KBMS;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

public class IQRoutersTest extends IQCamelTest {
protected final Logger log = LoggerFactory.getLogger(getClass());

//@Test
void testNLP() throws Exception {
GenericApplicationContext applicationContext = new GenericApplicationContext();
//applicationContext.setClassLoader(getClass().getClassLoader());
////applicationContext.
SpringCamelContext camelContext = new SpringCamelContext(applicationContext);
camelContext.getRegistry().bind("iq", new KBMS(NS.GG_TEST, repository.getConnection()));

log.info("iq.router.nlp: "+camelContext.getName());

IQRouters iqRouter = new IQRouters() {
@Override
public void configure() throws Exception {
log.info("iq.router.base: " + iq);
policies.add(new IQRoutePolicy(iq));

log.info("iq.router.camel.name: " + getContext().getName());

RouteDefinition test = from("servlet:test/select").
setProperty("matchOnUriPrefix", constant(true)).log("servlet-test").to("direct:rdf4j-select");

RouteDefinition rdf4j_select = from("direct:rdf4j-select")
.to("rdf4j:select:queries/concepts.sparql").marshal().json().log("rdf4j-select");

RouteDefinition rdf4j_construct = from("direct:rdf4j-construct")
.to("rdf4j:select:queries/infer.sparql").marshal().json().log("rdf4j-construct");

RouteDefinition from_nlp_servlet = from("servlet:nlp").log("rdf4j-nlp");
}
};
camelContext.addRoutes(iqRouter);
camelContext.start();
}
}