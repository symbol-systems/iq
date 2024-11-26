package systems.symbol.intent;

import org.jetbrains.annotations.NotNull;
import systems.symbol.fsm.StateException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.I_Bootstrap;

import javax.script.Bindings;
import java.util.Set;

/**
 * Abstract base class for intents.
 */
public abstract class AbstractIntent implements I_Intent, I_Bootstrap {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected Model model;
protected IRI self;

/**
 * Constructs a new AbstractIntent with the provided RDF4J model and self
 * identity.
 *
 */
protected AbstractIntent() {
}

/**
 * Initializes the AbstractIntent with the provided RDF4J model and self
 * identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public void boot(@NotNull IRI self, Model model) {
this.self = self;
this.model = model == null ? new DynamicModelFactory().createEmptyModel() : model;
}

/**
 * Retrieves the RDF4J model associated with the intent.
 *
 * @return The RDF4J model.
 */
public Model getModel() {
return model;
}

/**
 * Executes the intent based on the provided subject and object.
 *
 * @param actor The subject of the execution.
 * @param state The object of the execution.
 * @return A set of IRIs resulting from the execution.
 */
@Override
abstract public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException;

/**
 * Retrieves the self identity of the intent.
 *
 * @return The self identity as an IRI.
 */
public IRI getSelf() {
return this.self;
}

/**
 * Returns a string representation of the intent.
 *
 * @return The string representation of the intent.
 */
public String toString() {
return this.self.stringValue() + "@" + getClass().getCanonicalName();
}
}
