package systems.symbol.camel;

import org.junit.jupiter.api.Test;
import systems.symbol.COMMONS;
import systems.symbol.camel.routing.IQRoutePolicy;
import systems.symbol.camel.routing.IQRouters;
import systems.symbol.rdf4j.iq.KBMS;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

public class IQRoutersTest extends IQCamelTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

//  @Test
    void testNLP() throws Exception {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.refresh();
//        applicationContext.setClassLoader(getClass().getClassLoader());
////        applicationContext.
        SpringCamelContext camelContext = new SpringCamelContext(applicationContext);
        camelContext.getRegistry().bind("iq", new KBMS(vf.createIRI(COMMONS.GG_TEST), repository.getConnection()));

        log.info("iq.router.nlp: "+camelContext.getName());

        systems.symbol.agent.I_Agent agent = new systems.symbol.agent.I_Agent() {
            @Override
            public org.eclipse.rdf4j.model.Model getThoughts() {
                return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
            }

            @Override
            public systems.symbol.fsm.I_StateMachine<org.eclipse.rdf4j.model.Resource> getStateMachine() {
                return null;
            }

            @Override
            public org.eclipse.rdf4j.model.IRI getSelf() {
                return vf.createIRI(COMMONS.GG_TEST);
            }

            @Override
            public void start() throws Exception {
            }

            @Override
            public void stop() {
            }
        };
        systems.symbol.trust.IQ iqObject = new systems.symbol.trust.IQ(agent, new org.eclipse.rdf4j.model.impl.LinkedHashModel());

        IQRouters iqRouter = new IQRouters() {
            {
                this.iq = iqObject;
            }

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