package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static systems.symbol.intent.Learn.KNOWS;

public class Forget extends AbstractIntent {
//static final IRI FORGETS = Values.iri(COMMONS.IQ_NS, "forget");
public Forget(IRI self, Model model) {
super(model, self);
}

/**
 * A `forget` a fact and removes the `knows` from the actor.
 *
 * @param actor The actor/agent who is forgetting
 * @param fact  The fact that the actor should  'forget'
 * @return The set of forgotten facts (1 or 0)
 */
public Set<IRI> forget(IRI actor, Resource fact, Bindings _unused) throws IOException {
Set<IRI> done = new HashSet<>();
model.remove(actor, KNOWS, fact);
if (fact instanceof IRI) done.add((IRI)fact);
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
