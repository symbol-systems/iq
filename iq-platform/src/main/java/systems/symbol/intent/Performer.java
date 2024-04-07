package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.IRIs;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * The Performer class executes activities annotated with RDF metadata.
 * It allows adding instances of I_Intent, introspects their methods for RDF annotations,
 * and performs activities based on RDF triples.
 *
 * This class maintains a registry of RDF-annotated methods along with their corresponding IRI identifiers,
 * providing a mechanism to dynamically execute activities defined in the RDF vocabulary.
 *
 * @author Symbol Systems
 */
public class Performer implements I_Intent {
    private final Logger log = LoggerFactory.getLogger(getClass());
    Map<IRI, I_Intent> intents = new HashMap<>();
    IRI self;
    Model model;

    /**
     * Constructor for the Performer class.
     *
     * @param self The IRI identifier for the Performer itself.
     */
    public Performer(Model model, IRI self) {
        this.model = model;
        this.self = self;
    }

    public Performer(Model model, IRI self, I_Intent intent) {
        this.model = model;
        this.self = self;
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
    public Set<IRI> execute(Statement s) throws StateException {
        return execute((IRI) s.getSubject(), s.getPredicate(), (IRI) s.getObject());
    }

    /**
     * Performs an activity based on the provided IRIs (RDF triple).
     *
     * @param s The subject IRI - the input/source for the activity.
     * @param p The predicate IRI - used to identify the activity.
     * @param o The object IRI - the object/target of the activity.
     * @return A Set of IRIs representing the result of the activity or null if no activity.
     */
    public Set<IRI> execute(IRI s, IRI p, IRI o) throws StateException {
        // Retrieve the activity based on the predicate IRI
        I_Intent activity = this.intents.get(p);
        log.info("activity.perform: {} -> {}", p, activity);
        if (activity == null) return new IRIs();
        return activity.execute(s, o);
    }

    /**
     * Gets the IRI identity of the Performer.
     *
     * @return The IRI identifier of the Performer.
     */
    public IRI getIdentity() {
        return self;
    }

    public void execute(IRI subject, Iterable<Statement> intentions, Set<IRI> done) throws StateException {
        for (Statement triple : intentions) {
            Resource s = triple.getSubject();
            IRI p = triple.getPredicate();
            Value o = triple.getObject();
            if (s.isIRI() && o.isIRI() && p.isResource()) {
                log.info("perform.this: {} -> {} --> {}", s, p, o);
                Set<IRI> performed = execute(subject, p, (IRI) o);
                log.info("perform.done: {} -> {}", o, performed);
                if (!performed.isEmpty()) done.addAll(performed);
            } else {
                log.info("perform.skip: {} -> {} --> {}", s, p, o);
            }
        }
    }

    @Override
    @RDF(COMMONS.IQ_NS + "perform")
    public Set<IRI> execute(IRI subject, Resource state) throws StateException {
        IRIs done = new IRIs();
        Iterable<Statement> statements = model.getStatements(state, null, null);
        execute(subject, statements, done);
        return done;
    }

    public Set<IRI> perform() throws StateException {
        IRIs done = new IRIs();
        Iterable<Statement> statements = model.getStatements(getIdentity(), null, null);
        for (Statement statement : statements) {
            execute(statement);
        }
        return done;
    }
}
