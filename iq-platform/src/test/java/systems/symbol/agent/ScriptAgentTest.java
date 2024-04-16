package systems.symbol.agent;

import systems.symbol.fsm.ModelStateMachine;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.util.RDFPrefixer;


import javax.script.SimpleBindings;
import java.util.Optional;
import java.util.Set;

import static systems.symbol.fsm.ModelStateMachineTest.*;
import static systems.symbol.fsm.SimpleStateMachineTest.wip;

class ScriptAgentTest {
DynamicModelFactory dmf = new DynamicModelFactory();
Model model;
String groovyExample = "print 'hello world'; iq.set('hello', 'world'); return 'hello'";
IRI helloProperty = Values.iri(COMMONS.IQ_NS_TEST+"hello");

@BeforeEach
void setupAgentModel() {
model = dmf.createEmptyModel();
RDFPrefixer.defaultNamespaces(model);
newMSM(model,self);
Value scriptBody = Values.***REMOVED***(groovyExample, Values.iri("urn:"+COMMONS.MIME_GROOVY));
model.add(wip, RDF.VALUE, scriptBody);
}

@Test
void testExecution() throws Exception {
ScriptAgent agent = new ScriptAgent(model, self);
Set<IRI> executed = agent.execute(self, wip, new SimpleBindings());
System.out.println("agent.script.executed: "+executed);
RDFDump.dump(model);

Optional<Literal> propertyLiteral = Models.getPropertyLiteral(model, self, helloProperty);
assert propertyLiteral.isPresent();
assert propertyLiteral.get().stringValue().equals("world");
}

@Test
void testStateExecution() throws Exception {
ScriptAgent agent = new ScriptAgent(model, self);
ModelStateMachine workflow_0 = newMSM(model, LazyAgentTest.workflow_0);
agent.setFSM(workflow_0);
assert workflow_0.equals(agent.fsm);

Resource transitioned = agent.getStateMachine().transition(wip);
System.out.println("agent.state.executed: "+transitioned+" -> "+agent.getStateMachine().getState());
RDFDump.dump(model, System.out, RDFFormat.TURTLE);

Optional<Literal> propertyLiteral = Models.getPropertyLiteral(model, self, helloProperty);
System.out.println("agent.state.***REMOVED***: "+helloProperty);
assert propertyLiteral.isPresent();
assert propertyLiteral.get().stringValue().equals("world");

assert workflow_0.getState().equals(wip);
}
}