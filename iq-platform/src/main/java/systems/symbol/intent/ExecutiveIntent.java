package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.IRIs;

import javax.script.Bindings;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static systems.symbol.platform.Provenance.generated;

/**
 * The ExecutiveIntent class represents an intent executor capable of executing sets of operations based on the
 * I_Intents it is constructed with.
 *
 * When transitioning to a new state, it automatically executes the intents associated with that state.
 * If the state has exactly one next step, it automatically attempts to transition to the next step in its workflow.
 *
 * An ExecutiveIntent is composed of a collection of I_Intent implementations, each associated with a specific
 * action or operation.
 *
 * It has the capability to learn or forget facts and execute scripts and/or other intents in other states through indirection.
 */
public class ExecutiveIntent extends AbstractIntent implements I_Intents {
    Map<IRI, I_Intent> intents = new HashMap<>();
    Model facts;
    /**
     * An executive represents a set of I_Intents. During execution, matching intents are executed.
     * All Executives have the ability to learn/forget facts and `execute` Intents in other states (indirection).
     *
     * @param self The IRI identifier for the Executive itself.
     */
    public ExecutiveIntent(IRI self, Model facts) {
        boot(self, facts);
        this.facts = facts;
        log.info("exec.facts: {} -> {}", self, facts.size());
    }

    /**
     * An executive able to execute specified intents, and follow next-steps.
     * @param self The IRI identifier for the Executive itself.
     * @param thoughts The working memory
     * @param intent The I_Intent to perform we undergo a state transitions
     */
    public ExecutiveIntent(IRI self, Model facts, Model thoughts, I_Intent intent) {
        boot(self, thoughts);
        this.facts = facts;
        add(intent);
        log.info("exec.thoughts: {} -> {} & {}-> {}", self, facts.size(), thoughts.size(), intents.keySet());
    }

    public void boot(@NotNull IRI self, Model model) {
        add(this);
        super.boot(self, model);
        add(new Learn(self, model));
        add(new Forget(self, model));
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
        log.debug("executive.intents: {}", this.intents);
        // Add annotated methods
        for (Method method : methods) {
            log.debug("executive.method: {} -> {}", method.getName(), method.isAnnotationPresent(RDF.class));
            if (method.isAnnotationPresent(RDF.class)) {
                RDF methodRdfAnnotation = method.getAnnotation(RDF.class);
                IRI methodIRI = Values.iri(methodRdfAnnotation.value());
                log.debug("executive.method.rdf: {} -> {}", method.getName(), methodIRI);
//                if (this.intents.containsKey(methodIRI)) throw new RuntimeException(methodIRI + "#duplicate");
                this.intents.put(methodIRI, intent);
                return methodIRI;
            }
        }
        return null;
    }

    @Override
    public Set<IRI> getIntents() {
        return intents.keySet();
    }

    /**
     * Performs an intent for the RDF triple provided in the Statement.
     *
     * @param s The RDF Statement representing the triple.
     * @return A Set of IRIs representing the result of the intent.
     */
    protected Set<IRI> execute(Statement s, Bindings bindings) throws StateException {
        return executeIntent((IRI) s.getSubject(), s.getPredicate(), (IRI) s.getObject(), bindings);
    }

    /**
     * Performs an intent based on the provided IRIs (RDF triple).
     *
     * @param actor The subject IRI - the input/source for the intent.
     * @param p The predicate IRI - used to identify the intent.
     * @param o The object IRI - the object/target of the intent.
     * @return A Set of IRIs representing the result of the intent or null if no intent.
     */
    protected Set<IRI> executeIntent(IRI actor, IRI p, Resource o, Bindings bindings) throws StateException {
        I_Intent intent = this.intents.get(p);
        if (intent == null) return new IRIs();
        log.info("execute.intent: {} == {} -> {} == {}", actor, p, o, intent.getClass().getSimpleName());
        return intent.execute(actor, o, bindings);
    }

    /**
     * Find matching the I_Intents and execute each.
     */

    protected void executeMatchingIntents(IRI actor, Iterable<Statement> maybes, Set<IRI> done, Bindings bindings) throws StateException {
        for (Statement maybe : maybes) {
            Resource s = maybe.getSubject();
            IRI p = maybe.getPredicate();
            Value o = maybe.getObject();
            if (s.isIRI() && o.isResource() && p.isResource()) {
                done.addAll( executeIntent(actor, p, (Resource) o, bindings) );
            }
        }
    }

    @Override
    @RDF(IQ_NS.IQ + "do")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        IRIs done = new IRIs();
        log.info("execute.intents: {}", intents.keySet());
        Iterable<Statement> statements = facts.getStatements(state, null, null);
        executeMatchingIntents(actor, statements, done, bindings);
        if (model!=facts) {
            statements = model.getStatements(state, null, null);
            executeMatchingIntents(actor, statements, done, bindings);
        }
        log.info("execute.done: {}", done);
        return done;
    }
}
