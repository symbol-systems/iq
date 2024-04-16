package systems.symbol.agent;

import systems.symbol.fsm.ModelStateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import static systems.symbol.fsm.ModelStateMachineTest.*;
import static systems.symbol.fsm.SimpleStateMachineTest.ideation;
import static systems.symbol.fsm.SimpleStateMachineTest.wip;

class LazyAgentTest {
DynamicModelFactory dmf = new DynamicModelFactory();
static ValueFactory vf = SimpleValueFactory.getInstance();
static IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);
static IRI workflow_0 = vf.createIRI(COMMONS.IQ_NS_TEST +"skill-0");

@Test
void testFSM() throws StateException {
Model model = dmf.createEmptyModel();
AbstractAgent agent = new LazyAgent(model, self);

ModelStateMachine workflow_0 = newMSM(model, LazyAgentTest.workflow_0);
assert LazyAgentTest.workflow_0.equals(workflow_0.getSelf());

agent.learn(workflow_0);
assert workflow_0.equals(agent.getStateMachine());

Resource done = agent.getStateMachine().transition(wip);
System.out.println("agent.iq: "+done);
assert agent.getStateMachine().getState().equals(wip);
assert done != null;
System.out.println("agent.fsm.done: "+agent.getSelf()+ " -> "+agent.getStateMachine());
}



@Test
void testTransition() throws StateException {
Model model = dmf.createEmptyModel();
boolean[] acted = {false};
LazyAgent agent = new LazyAgent(model, self) {
@Override
public boolean onTransition(Resource from, Resource to) {
return acted[0] = super.onTransition(from, to);
}
};
ModelStateMachine workflow_0 = newMSM(model, LazyAgentTest.workflow_0);
agent.learn(workflow_0);

System.out.println("agent.lazy.state: "+workflow_0.getState());
assert ideation.equals(workflow_0.getState());
workflow_0.transition(wip);
System.out.println("agent.lazy.done: "+workflow_0.getState()+" => "+acted[0]);
assert acted[0];
assert workflow_0.getState().equals(wip);
}
}