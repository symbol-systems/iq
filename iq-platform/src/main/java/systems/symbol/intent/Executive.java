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

import static systems.symbol.platform.Provenance.generated;

/**
 * The Executive executes operations based on the I_Intents it is equipped with.
 * When the Executive transitions to a new state, it will automatically execute that state's intents.
 * If that state has a single child, it will automatically attempt to transition to the next step in its workflow.
 *
 * @author Symbol Systems
 */
public class Executive extends AbstractIntent implements I_Intents {
private final Logger log = LoggerFactory.getLogger(getClass());
Map<IRI, I_Intent> intents = new HashMap<>();

/**
 * An executive able to learn/forget facts, and follow next-steps.
 *
 * @param self The IRI identifier for the Executive itself.
 */
public Executive(IRI self, Model model) {
super(self, model);
memorize();
}

/**
 * An executive able to execute specified intents, and follow next-steps.
 * @param self The IRI identifier for the Executive itself.
 * @param model The working memory
 * @param intent The I_Intent to perform we undergo a state transitions
 */
public Executive(IRI self, Model model, I_Intent intent) {
super(self, model);
add(intent);
memorize();
}

public void memorize() {
add(new Knows(self, model));
add(new Forget(self, model));
log.info("memorize: {} -> {}", self, intents.keySet());
}

/**
 * Adds an I_Intent intent by indexing methods with RDF annotations.
 *
 * @param intent The I_Intent instance to be added.
 * @return The IRI for the intent or NULL if not annotated.
 * @throws RuntimeException if a duplicate IRI is encountered.
 */
public IRI add(I_Intent intent) {
if (intent == null) return null;
Method[] methods = intent.getClass().getDeclaredMethods();
// Add annotated methods
for (Method method : methods) {
log.debug("method.add: {} -> {}", method.getName(), method.isAnnotationPresent(RDF.class));
if (method.isAnnotationPresent(RDF.class)) {
RDF methodRdfAnnotation = method.getAnnotation(RDF.class);
IRI methodIRI = Values.iri(methodRdfAnnotation.value());
log.debug("method.rdf: {} -> {}", method.getName(), methodIRI);
if (intents.containsKey(methodIRI)) throw new RuntimeException(methodIRI + "#duplicate");
intents.put(methodIRI, intent);
return methodIRI;
}
}
return null;
}

/**
 * Performs an intent for the RDF triple provided in the Statement.
 *
 * @param s The RDF Statement representing the triple.
 * @return A Set of IRIs representing the result of the intent.
 */
protected Set<IRI> execute(Statement s, Bindings bindings) throws StateException {
return execute((IRI) s.getSubject(), s.getPredicate(), (IRI) s.getObject(), bindings);
}

/**
 * Performs an intent based on the provided IRIs (RDF triple).
 *
 * @param s The subject IRI - the input/source for the intent.
 * @param p The predicate IRI - used to identify the intent.
 * @param o The object IRI - the object/target of the intent.
 * @return A Set of IRIs representing the result of the intent or null if no intent.
 */
protected Set<IRI> execute(IRI s, IRI p, IRI o, Bindings bindings) throws StateException {
I_Intent intent = this.intents.get(p);
if (intent == null) return new IRIs();
log.info("execute.intent: {} @ {}", p, intent);
return intent.execute(s, o, bindings);
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
log.info("execute.intents: {} -> {}", state, statements.iterator().hasNext());
execute(actor, statements, done, bindings);
done.forEach(result -> {
generated(model, actor, state, result, getSelf());
});
return done;
}
}
