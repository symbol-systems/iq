package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static systems.symbol.platform.IQ_NS.KNOWS;

public class Learn extends AbstractIntent {

public Learn(IRI self, Model model) {
super(self, model);
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
