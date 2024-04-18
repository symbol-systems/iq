package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;

import javax.script.Bindings;
import java.io.IOException;
import java.util.*;

public class Learn extends AbstractIntent {

public final static IRI KNOWS = Values.iri(COMMONS.IQ_NS, "knows");
public Learn(IRI self, Model model) {
super(model, self);
}

/**
// * Learn a fact and transfer to the actor as `knows`
 *
 * @param actor  The actor/agent who is learning
 * @param fact  The fact that the actor should 'learn'
 * @return The set of learned facts (1 or 0)
 */
public Set<IRI> learn(IRI actor, Resource fact, Bindings _unused) throws IOException {
Set<IRI> done = new HashSet<>();

log.info("learn: {} -> {}", actor, fact);
model.add(actor, KNOWS, fact, getSelf());
if (fact instanceof IRI) done.add((IRI)fact);
return done;
}


@Override
@RDF(COMMONS.IQ_NS+"learn")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return learn(actor, state, bindings);
} catch (IOException e) {
throw new StateException(e.getMessage(), state, e);
}
}

}
