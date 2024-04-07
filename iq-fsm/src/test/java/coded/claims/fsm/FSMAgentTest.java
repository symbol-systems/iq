package systems.symbol.fsm;

import systems.symbol.camel.component.FSMAgentComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.jeasy.states.api.FiniteStateMachine;
import org.jeasy.states.api.State;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FSMAgentTest {

    @BeforeMethod
    public void setUp() {
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testStates() throws Exception {
        CamelContext camel = new DefaultCamelContext();
        FSMAgentComponent fsmAgentComponent = new FSMAgentComponent();
        camel.addComponent("fsm", fsmAgentComponent);

        FSMAgent agent = new FSMAgent();
        State locked = agent.state("locked");
        State unlocked = agent.state("unlocked");
        agent.transition("unlock", locked, unlocked, event -> System.out.println("iq.camel.fsm.event:"+event.getName()));
        agent.transition("lock", unlocked, locked);
        FiniteStateMachine fsm_door = agent.fsm(locked);
        fsmAgentComponent.register("door", fsm_door);

        RouteBuilder routes = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("fsm:door/one").to("log:fsm-door-one");
            }
        };
        camel.addRoutes(routes);
        camel.start();
        DefaultProducerTemplate producerTemplate = new DefaultProducerTemplate(camel);
        producerTemplate.start();

        Object done = producerTemplate.requestBody("fsm:door/one/unlock", "");
        System.out.println("iq.camel.fsm.done: "+done);
        camel.stop();
    }
}