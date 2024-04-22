package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.finder.I_FactFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Set;

import static systems.symbol.platform.IQ_NS.KNOWS;

/**
 * An intent implementation that executes scripts using SPARQL (Java Scripting API).
 * Extends the AbstractIntent class.
 */
public class Search extends AbstractIntent {
private final I_FactFinder finder;

/**
 * Constructs a new SPARQL intent with the provided Connection and self identity.
 *
 * @param self  The self identity of the agent.
 */
public Search(IRI self, Model model, I_FactFinder finder) {
boot(self, model);
this.finder = finder;
}

/**
 * Creates a new bindings object for script execution.
 * Executes the SPARQL query based on the provided actor and resource.
 *
 * @param actor   The actor of the execution.
 * @param state  The resource containing the script.
 * @param ctxBindings used in query interpolation.
 * @return A set of IRIs indicating the completion of execution.
 */
@Override
@RDF(COMMONS.IQ_NS + "search")
public Set<IRI> execute(IRI actor, Resource state, Bindings ctx) throws StateException {
Literal prompt = IQScripts.findScript(model, state, null, null);
if (prompt==null || prompt.stringValue().isEmpty()) return new IRIs();
try {
Bindings bindings = MyFacade.rebind(actor, ctx);
String query = HBSRenderer.template(prompt.stringValue(), bindings);
Model found = finder.search(query);
Set<IRI> iris = Models.subjectIRIs(found);
for(IRI iri:iris) {
model.add(actor, KNOWS, iri);
}
return iris;
} catch (IOException e) {
throw new StateException(e.getMessage(), state, e);
}
}

@Override
public IRI getSelf() {
return self;
}
}
