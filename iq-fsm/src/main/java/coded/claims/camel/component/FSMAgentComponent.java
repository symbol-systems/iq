package systems.symbol.camel.component;

import systems.symbol.camel.GenericEndpoint;
import systems.symbol.camel.processor.fsm.FSMAgentProcessor;
import systems.symbol.util.Steps;
import org.apache.camel.CamelException;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.jeasy.states.api.FiniteStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel
 * @author Symbol Systems
 * Date  : 22/06/2014
 * Time  : 11:51 PM
 */
@Component
public class FSMAgentComponent extends DefaultComponent {
	static protected final Logger log = LoggerFactory.getLogger(RDF4JComponent.class);
	Map<String, FiniteStateMachine> fsms = new HashMap<>();

	public FSMAgentComponent() {
	}

	public void register(String name, FiniteStateMachine fsm) {
		fsms.put(name, fsm);
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		Steps steps = new Steps(remaining);
		String fsm_type = steps.step();
		String fsm_id = steps.step();
		String fsm_action = steps.step();
		log.info("iq.camel.fsm.endpoint: "+steps+" -> "+fsm_type+" -> "+fsm_action+" @ "+fsm_id);
		FiniteStateMachine fsm = fsms.get(fsm_type);
		if (fsm==null) throw new CamelException("iq.camel.fsm.unknown: "+fsm_type);
		return new GenericEndpoint(uri, new FSMAgentProcessor(fsm));
	}
}
