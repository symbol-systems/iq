package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.jetbrains.annotations.NotNull;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of an agent that executes intents when a state change occurs.
 */
public class IntentAgent extends AbstractAgent {
protected final I_Intent intent;
protected Bindings bindings;
/**
 * Constructs a new IntentAgent with the provided intent, RDF4J model, and self identity.
 *
 * @param intent The intent to be executed by the agent.
 * @param model  The RDF4J model associated with the agent.
 * @param self   The self identity of the agent.
 */
public IntentAgent(@NotNull IRI self, @NotNull Model model, @NotNull I_Intent intent, @NotNull Bindings bindings) throws StateException {
super(self, model);
this.intent = intent;
this.bindings = bindings;
}
/**
 * Handles transitions within a symbolic system.
 *
 * @param from The resource representing the source state of the transition.
 * @param to   The resource representing the target state of the transition.
 * @return true if the transition is handled successfully, false otherwise.
 */
@Override
public boolean onTransition(Resource from, Resource to) throws StateException {
try {
log.debug("onTransition: {}", to);
execute(getSelf(), to, bindings);
return true;
} catch (StateException e) {
return false;
}
}

/**
 * Executes an intent based on the provided subject and object.
 *
 * @param actor The subject of the execution (the actor).
 * @param state  The object of the execution (the intent).
 * @return A set of IRIs resulting from the execution.
 * @throws StateException If an error occurs during execution.
 */
@Override
@RDF(IQ_NS.IQ + "agent")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
if (state instanceof IRI) {
log.info("agent.do: {} <-- {} @ {}", intent, actor, state);
return intent.execute(actor, state, bindings);
}
Set<IRI> iris = new HashSet<>();
Collection<Statement> found = RDFCollections.getCollection(thoughts, state, new HashSet<>());
for (Statement statement : found) {
Value v = statement.getObject();
if (v instanceof Resource) {
iris.addAll( execute(actor, (Resource) v, bindings) );
}
}
return iris;
}
}
