package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.lake.BootstrapRepository;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.store.IQConnection;

import javax.script.Bindings;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class RDFAgentTest {
public static BootstrapRepository assets;
public static ValueFactory vf;
public static IRI self, iriGroovyScript, iriWorkflow0;
IRI ideation = vf.createIRI(IQ_NS.TEST + "ideation");
IRI wip = vf.createIRI(IQ_NS.TEST + "work-in-progress");

@BeforeAll
public static void setUp() throws IOException {
assets = new BootstrapRepository();
self = assets.load(new File("src/test/resources/assets"), IQ_NS.TEST);
vf = assets.getValueFactory();
iriWorkflow0 = vf.createIRI(IQ_NS.TEST + "workflow_0");
iriGroovyScript = vf.createIRI(IQ_NS.TEST + "scripts/hello");

}

@Test
public void testLoadedScript() {
try (RepositoryConnection connection = assets.getConnection()) {
IQConnection iq = new IQConnection(self, connection);
IQScriptCatalog scripts = new IQScriptCatalog(iq);

Literal script = scripts.getContent(iriGroovyScript, null);
System.out.println("agent.rdf.script: " + iriGroovyScript + " -> " + script);
assert script != null;
assert script.stringValue().contains("return ");
}
}

@Test
public void testLoadedFSM() throws Exception {
try (RepositoryConnection connection = assets.getConnection()) {

GraphQuery query = connection.prepareGraphQuery("CONSTRUCT { ?s ?p ?o } WHERE {?s ?p ?o}");
Model model = SPARQLMapper.toModel(query.evaluate());
assert null != model;
assert model.size() > 90;

RDFDump.dump(model, System.out, RDFFormat.TURTLE);

boolean[] actioned = { false };
System.out.println("agent.rdf.self: " + self);
LazyAgent agent = new LazyAgent(self, model) {
public boolean onTransition(Resource from, Resource to) {
return actioned[0] = true;
}

@Override
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
return new IRIs();
}
};
I_StateMachine<Resource> fsm = agent.getStateMachine();
assert null != fsm;
System.out.println("agent.rdf.fsm: " + fsm.getTransitions());
assert null != fsm.getState();
assert ideation.equals(fsm.getState());
Resource wip_ok = fsm.transition(wip);
assert fsm.getState().equals(wip_ok);
// System.out.println("agent.rdf.ok: "+wip_ok+" --> "+wip);
assert wip_ok.equals(wip);
assert fsm.getTransitions().size() == 1;
assert actioned[0];
System.out.println("agent.rdf.done");
}
}
}
