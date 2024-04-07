package systems.symbol.intent;

import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for intents within a symbolic system.
 */
public abstract class AbstractIntent implements I_Intent {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected Model model;
protected IRI self;

/**
 * Constructs a new AbstractIntent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
protected AbstractIntent(Model model, IRI self) {
init(model, self);
}

/**
 * Initializes the AbstractIntent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public void init(Model model, IRI self) {
this.model = model == null ? new DynamicModelFactory().createEmptyModel() : model;
this.self = self;
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
 * @param subject The subject of the execution.
 * @param object  The object of the execution.
 * @return A set of IRIs resulting from the execution.
 */
@Override
abstract public Set<IRI> execute(IRI subject, Resource object);

/**
 * Retrieves the self identity of the intent.
 *
 * @return The self identity as an IRI.
 */
public IRI getIdentity() {
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
