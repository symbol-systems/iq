package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static systems.symbol.intent.Knows.KNOWS;

public class Forget extends AbstractIntent {
static final IRI FORGETS = Values.iri(COMMONS.IQ_NS, "forget");
public Forget(IRI self, Model model) {
super(model, self);
}

/**
 * Bind SPARQL results as data and render a single state into a new Literal
 *
 * @param actor   actor source of models
 * @param state  state for each model
 * @return Set of one IRI for the new triple
 */
public Set<IRI> forget(IRI actor, Resource state, Bindings _unused) throws IOException {
Set<IRI> done = new HashSet<>();
model.remove(actor, KNOWS, state);
if (state instanceof IRI) done.add((IRI)state);
return done;
}


@Override
@RDF(COMMONS.IQ_NS+"forget")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return forget(actor, state, bindings);
} catch (IOException e) {
throw new StateException(e.getMessage(), state, e);
}
}

}
