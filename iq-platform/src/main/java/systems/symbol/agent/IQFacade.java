package systems.symbol.agent;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.util.RDFPrefixer;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;

import javax.script.Bindings;
import java.util.ArrayList;
import java.util.List;

/**
 * A simplified string-friendly wrapper for scripting an agent, model and state machine.
 */
public class IQFacade {
private final Model model;
private final IRI self;

/**
 * Constructs a new ScriptFacade with the provided RDF4J model and self identity,
 * creating a LazyAgent as the base agent.
 *
 * @param model The RDF4J model associated with the facade.
 * @param self  The self identity of the facade.
 */
public IQFacade(@NotNull IRI self, @NotNull Model model) throws StateException {
this.model = model;
this.self = self;
}

/**
 * Sets an `rdf:value`.
 *
 * @param value The value of the property.
 * @return The ScriptFacade instance.
 */
public IQFacade value(Object value) {
model.add(self, RDF.VALUE, Values.***REMOVED***(value), this.self);
return this;
}

public Literal value() {
return get(RDF.VALUE).get(0);
}

/**
 * Sets a property in the model.
 *
 * @param key   The key of the property.
 * @param value The value of the property.
 * @return The ScriptFacade instance.
 */
public IQFacade set(String key, Object value) {
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
return get(toIRI(key));
}

protected List<Literal> get(IRI predicate) {
List<Literal> ***REMOVED***s = new ArrayList<>();
Iterable<Statement> statements = model.getStatements(self, predicate, null, this.self);
for (Statement statement : statements) {
if (statement.getObject() instanceof Literal) {
***REMOVED***s.add((Literal) statement.getObject());
}
}
return ***REMOVED***s;
}


public static Bindings rebind(IRI self, Model model, Bindings my) throws StateException {
Bindings bindings = MyFacade.rebind(self, my);
bindings.put(MyFacade.IQ, new IQFacade(self, model));
return bindings;
}
}
