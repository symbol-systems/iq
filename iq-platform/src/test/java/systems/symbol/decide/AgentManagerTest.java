package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.IntentAgent;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.SimpleStateMachineTest;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.platform.IQ_NS;

import javax.script.SimpleBindings;
import java.util.Collection;

import static systems.symbol.fsm.SimpleStateMachineTest.*;

class AgentManagerTest {
DynamicModelFactory dmf = new DynamicModelFactory();
public static IRI self = Values.iri(IQ_NS.TEST);

@Test
void testDecisions() throws StateException {
Model model = dmf.createEmptyModel();

IntentAgent agent = new IntentAgent(self, model, new ExecutiveIntent(self, model), new SimpleBindings());
I_StateMachine<Resource> fsm = agent.getStateMachine();
SimpleStateMachineTest.addFSM(fsm);

// ideation -> wip -> review
assert fsm.getState().equals(ideation);
assert fsm.isAllowed(wip);
Resource transitioned = fsm.transition(wip);
assert transitioned == wip;
assert fsm.isAllowed(review);
fsm.transition(review);
assert fsm.isAllowed(complete);
assert fsm.isAllowed(revision);

// a decision with two choices
assert fsm.getTransitions().size() == 2;
// review --> complete
AgentManager human = new AgentManager(agent);
assert human.decisions.containsKey(agent);
Collection<Resource> intents = human.getIntents(agent);
System.out.println("agent.intents: "+fsm.getState()+" -> "+intents);
assert intents != null;
assert intents.contains(complete);
assert intents.contains(revision);
human.decide(agent, complete);
System.out.println("agent.complete: "+fsm.getState());
assert fsm.getState().equals(complete);
}
}