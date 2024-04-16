package systems.symbol.agent;

import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.IRIs;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.Set;

/**
 * Implementation of a lazy agent within a symbolic system.
 * This agent performs no action upon transition and simply returns the current state.
 */
public class LazyAgent extends AbstractAgent {

/**
 * Constructs a new LazyAgent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the agent.
 * @param self  The self identity of the agent.
 */
public LazyAgent(Model model, IRI self) throws StateException {
super(model, self);
}

/**
 * Handles transitions within the symbolic system.
 * This agent performs no action upon transition.
 *
 * @param from The resource representing the source state of the transition.
 * @param to   The resource representing the target state of the transition.
 * @return true if the transition is considered successful, false otherwise.
 */
@Override
public boolean onTransition(Resource from, Resource to) {
try {
Set<IRI> executed = execute(getSelf(), to, new SimpleBindings());
return !executed.isEmpty();
} catch (StateException e) {
log.warn("execution.failed: {}", to, e);
return false;
}
}

/**
 * Executes the agent's behavior based on the provided subject and state.
 * For a lazy agent, this method simply returns the current state.
 *
 * @param actor The subject of the execution.
 * @param state   The current state of the agent.
 * @return A set of IRIs representing the current state.
 * @throws StateException If an error occurs during execution.
 */
@Override
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
IRIs iris = new IRIs();
// do nothing
if (state instanceof IRI)  iris.add((IRI) state);
return iris;
}
}
