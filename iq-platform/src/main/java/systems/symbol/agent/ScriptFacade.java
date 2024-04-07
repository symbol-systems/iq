package systems.symbol.agent;

import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.util.RDFPrefixer;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Facilitates scripting interactions with an agent and its associated RDF4J model.
 */
public class ScriptFacade implements I_Decision {
private final Model model;
private final I_Agent agent;
private final IRI self;

/**
 * Constructs a new ScriptFacade with the provided RDF4J model and self identity,
 * creating a LazyAgent as the base agent.
 *
 * @param model The RDF4J model associated with the facade.
 * @param self  The self identity of the facade.
 */
public ScriptFacade(@NotNull Model model, @NotNull IRI self) {
this.agent = new LazyAgent(model, self);
this.model = model;
this.self = self;
}

/**
 * Constructs a new ScriptFacade with the provided agent, RDF4J model, and self identity.
 *
 * @param agent The base agent associated with the facade.
 * @param model The RDF4J model associated with the facade.
 * @param self  The self identity of the facade.
 */
public ScriptFacade(@NotNull I_Agent agent, @NotNull Model model, @NotNull IRI self) {
this.agent = agent;
this.model = model;
this.self = self;
}

/**
 * Sets a property in the RDF4J model associated with the facade.
 *
 * @param key   The key of the property.
 * @param value The value of the property.
 * @return The ScriptFacade instance.
 */
public ScriptFacade set(String key, Object value) {
model.add(self, toIRI(key), Values.***REMOVED***(value), this.self);
return this;
}

/**
 * Expand a prefixed string key to an IRI or appends a simple key to self IRI.
 *
 * @param key The string key to convert.
 * @return The IRI representation of the key.
 */
public IRI toIRI(String key) {
return RDFPrefixer.toIRI(model, self, key);
}

/**
 * Retrieves ***REMOVED***s associated with a property in the RDF4J model.
 *
 * @param key The key of the property.
 * @return A list of ***REMOVED***s associated with the property.
 */
public List<Literal> get(String key) {
List<Literal> ***REMOVED***s = new ArrayList<>();

Iterable<Statement> statements = model.getStatements(self, toIRI(key), null, this.self);
for (Statement statement : statements) {
if (statement.getObject() instanceof Literal) {
***REMOVED***s.add((Literal) statement.getObject());
}
}

return ***REMOVED***s;
}

/**
 * Makes a decision based on the provided state.
 *
 * @param state The state for decision-making.
 * @return The decision as a Resource.
 * @throws StateException If an error occurs during decision-making.
 */

public Resource decide(String state) throws StateException {
return decide(toIRI(state));
}

/**
 * Makes a decision based on the provided state.
 *
 * @param state The state for decision-making.
 * @return The decision as a Resource.
 * @throws StateException If an error occurs during decision-making.
 */
@Override
public Resource decide(Resource state) throws StateException {
if (agent.getStateMachine() == null) return null;
return agent.getStateMachine().transition(state);
}
}
