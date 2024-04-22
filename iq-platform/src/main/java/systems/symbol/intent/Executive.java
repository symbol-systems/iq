package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.IRIs;

import javax.script.Bindings;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static systems.symbol.platform.Provenance.generated;

/**
 * The Executive executes operations based on the I_Intents it is equipped with.
 * When the Executive transitions to a new state, it will automatically execute that state's intents.
 * If that state has a single child, it will automatically attempt to transition to the next step in its workflow.
 *
 * @author Symbol Systems
 */
public class Executive extends AbstractIntent implements I_Intents {
//    private final Logger log = LoggerFactory.getLogger(getClass());
    Map<IRI, I_Intent> intents = new HashMap<>();

    /**
     * An executive represents a set of I_Intents. During execution, matching intents are executed.
     * All Executives have the ability to learn/forget facts and `execute` Intents in other states (indirection).
     *
     * @param self The IRI identifier for the Executive itself.
     */
    public Executive(IRI self, Model model) {
        boot(self, model);
        log.info("booted: {} -> {}", self, intents.keySet());
    }

    /**
     * An executive able to execute specified intents, and follow next-steps.
     * @param self The IRI identifier for the Executive itself.
     * @param model The working memory
     * @param intent The I_Intent to perform we undergo a state transitions
     */
    public Executive(IRI self, Model model, I_Intent intent) {
        boot(self, model);
        add(intent);
        log.info("booted: {} -> {}", self, intents.keySet());
    }

    public void boot(@NotNull IRI self, Model model) {
        super.boot(self, model);
        add(new Learn(self, model));
        add(new Forget(self, model));
        add(this);
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
        log.info("discover.intents: {}", this.intents);
        // Add annotated methods
        for (Method method : methods) {
            log.debug("discover.method: {} -> {}", method.getName(), method.isAnnotationPresent(RDF.class));
            if (method.isAnnotationPresent(RDF.class)) {
                RDF methodRdfAnnotation = method.getAnnotation(RDF.class);
                IRI methodIRI = Values.iri(methodRdfAnnotation.value());
                log.info("discover.method.rdf: {} -> {}", method.getName(), methodIRI);
                if (this.intents.containsKey(methodIRI)) throw new RuntimeException(methodIRI + "#duplicate");
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
    protected Set<IRI> executeIntent(IRI actor, IRI p, IRI o, Bindings bindings) throws StateException {
        I_Intent intent = this.intents.get(p);
        if (intent == null) return new IRIs();
        log.info("execute.intent: {} @ {}", p, o);
        Set<IRI> executed = intent.execute(actor, o, bindings);
        provenance(executed, actor, o);
        return executed;
    }

    /**
     * Find matching the I_Intents and execute each.
     */

    protected void executeMatchingIntents(IRI actor, Iterable<Statement> maybes, Set<IRI> done, Bindings bindings) throws StateException {
        for (Statement maybe : maybes) {
            Resource s = maybe.getSubject();
            IRI p = maybe.getPredicate();
            Value o = maybe.getObject();
            if (s.isIRI() && o.isIRI() && p.isResource()) {
                done.addAll( executeIntent(actor, p, (IRI) o, bindings) );
            }
        }
        log.info("execute.done: {}", done);
    }

    void provenance(Set<IRI> done, IRI actor, Resource state) {
        done.forEach(result -> {
            generated(model, actor, state, result, getSelf());
        });
    }
    @Override
    @RDF(COMMONS.IQ_NS + "execute")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        IRIs done = new IRIs();
        Iterable<Statement> statements = model.getStatements(state, null, null);
        log.info("execute.intents: {} -> {} -> {}", state, statements.iterator().hasNext(), intents.keySet());
        executeMatchingIntents(actor, statements, done, bindings);
        return done;
    }
}
