package systems.symbol.agent;

import systems.symbol.fsm.ModelStateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Optional;
import java.util.Set;

import static systems.symbol.fsm.ModelStateMachineTest.*;

class ScriptAgentTest {
    DynamicModelFactory dmf = new DynamicModelFactory();
    ValueFactory vf = SimpleValueFactory.getInstance();
    Model model;
    String groovyExample = "print 'hello world'; iq.set('hello', 'world'); return 'hello'";
    IRI helloProperty = Values.iri(COMMONS.IQ_NS_TEST+"hello");

    @BeforeEach
    void setupAgentModel() {
        model = dmf.createEmptyModel();
        newMSM(model,self);
        Value scriptBody = vf.createLiteral(groovyExample, vf.createIRI(COMMONS.MIME_GROOVY));
        model.add(wip, RDF.VALUE, scriptBody);
    }

    @Test
    void testExecution() throws Exception {
        ScriptAgent agent = new ScriptAgent(model, self);
        Set<IRI> executed = agent.execute(self, wip);
        System.out.println("agent.script.executed: "+executed);

        Optional<Literal> propertyLiteral = Models.getPropertyLiteral(model, self, helloProperty);
        assert propertyLiteral.isPresent();
        assert propertyLiteral.get().stringValue().equals("world");
    }

    @Test
    void testStateExecution() throws Exception {
        ScriptAgent agent = new ScriptAgent(model, self);
        ModelStateMachine workflow_0 = newMSM(model, LazyAgentTest.workflow_0);
        agent.learn(workflow_0);
        assert workflow_0.equals(agent.fsm);

        Resource transitioned = agent.getStateMachine().transition(wip);
        System.out.println("agent.state.executed: "+transitioned+" -> "+agent.getStateMachine().getState());
        RDFDump.dump(model, System.out, RDFFormat.TURTLE);

        Optional<Literal> propertyLiteral = Models.getPropertyLiteral(model, self, helloProperty);
        assert propertyLiteral.isPresent();
        assert propertyLiteral.get().stringValue().equals("world");

        assert workflow_0.getState().equals(wip);
    }
}