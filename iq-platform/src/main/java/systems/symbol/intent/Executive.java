package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.IRIs;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The Executive aggregates.
 * It allows adding instances of I_Intent, introspects their methods for RDF annotations,
 * and performs activities based on RDF triples.
 *
 * @author Symbol Systems
 */
public class Executive implements I_Intent {
private final Logger log = LoggerFactory.getLogger(getClass());
Map<IRI, I_Intent> intents = new HashMap<>();
IRI self;
Model model;

/**
 * Constructor for the Performer class.
 *
 * @param self The IRI identifier for the Performer itself.
 */
public Executive(IRI self, Model model) {
this.self = self;
this.model = model;
}

public Executive(IRI self, Model model, I_Intent intent) {
this.self = self;
this.model = model;
add(intent);
}

/**
 * Adds an I_Activity activity and indexes any methods with RDF annotations.
 *
 * @param activity The I_Activity instance to be added.
 * @return The IRI for the activity or NULL if not annotated.
 * @throws RuntimeException if a duplicate IRI is encountered.
 */
public IRI add(I_Intent activity) {
if (activity == null) return null;
Method[] methods = activity.getClass().getDeclaredMethods();
// Add annotated methods
for (Method method : methods) {
log.info("method.add: {} -> {}", method.getName(), method.isAnnotationPresent(RDF.class));
if (method.isAnnotationPresent(RDF.class)) {
RDF methodRdfAnnotation = method.getAnnotation(RDF.class);
IRI methodIRI = Values.iri(methodRdfAnnotation.value());
log.info("method.rdf: {} -> {}", method.getName(), methodIRI);
if (intents.containsKey(methodIRI)) throw new RuntimeException(methodIRI + "#duplicate");
intents.put(methodIRI, activity);
return methodIRI;
}
}
return null;
}

/**
 * Performs an activity for the RDF triple provided in the Statement.
 *
 * @param s The RDF Statement representing the triple.
 * @return A Set of IRIs representing the result of the activity.
 */
protected Set<IRI> execute(Statement s, Bindings bindings) throws StateException {
return execute((IRI) s.getSubject(), s.getPredicate(), (IRI) s.getObject(), bindings);
}

/**
 * Performs an activity based on the provided IRIs (RDF triple).
 *
 * @param s The subject IRI - the input/source for the activity.
 * @param p The predicate IRI - used to identify the activity.
 * @param o The object IRI - the object/target of the activity.
 * @return A Set of IRIs representing the result of the activity or null if no activity.
 */
protected Set<IRI> execute(IRI s, IRI p, IRI o, Bindings bindings) throws StateException {
// Retrieve the activity based on the predicate IRI
I_Intent activity = this.intents.get(p);
log.info("execute: {} @ {}", p, activity);
if (activity == null) return new IRIs();
return activity.execute(s, o, bindings);
}

/**
 * Find matching the I_Intents and execute each.
 */

protected void execute(IRI subject, Iterable<Statement> intentions, Set<IRI> done, Bindings bindings) throws StateException {
for (Statement triple : intentions) {
Resource s = triple.getSubject();
IRI p = triple.getPredicate();
Value o = triple.getObject();
if (s.isIRI() && o.isIRI() && p.isResource()) {
Set<IRI> performed = execute(subject, p, (IRI) o, bindings);
if (!performed.isEmpty()) done.addAll(performed);
} else {
log.info("execute.skip: {} -> {} --> {}", s, p, o);
}
}
log.info("execute.done: {}", done);
}

@Override
@RDF(COMMONS.IQ_NS + "execute")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
IRIs done = new IRIs();
Iterable<Statement> statements = model.getStatements(state, null, null);
log.info("execute.found: {} -> {}", state, statements.iterator().hasNext());
execute(actor, statements, done, bindings);
return done;
}

public IRI getSelf() {
return self;
}
}
