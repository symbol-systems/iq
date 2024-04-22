package systems.symbol.fsm;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import systems.symbol.platform.I_Self;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

import static systems.symbol.platform.IQ_NS.*;

/**
 * State machine (FSM) uses RDF4J Model for state representation.
 * RDF4J's Model to store information about states, transitions, and guards.
 * The class initializes with a specified RDF model and self-reference IRI
 * the initial state is set to the first transition added
 * checks if transitions are allowed based on defined guard rules.
 *
 */
public class ModelStateMachine extends AbstractStateMachine<Resource> implements I_Self {
    private final Model model;
    protected IRI self;

    /**
     * A finite state machine backed by an RDF4J Model to store information about states, transitions, and guards.
     * The class initializes with a specified RDF model and self-reference IRI, extracting the initial state.
     * Checks if transitions are allowed based on defined guards.
     * Guards are "exemplars" meaning their predicates/objects must be matched by `self` statements
     *
     * @param model RDF4J Model containing state machine information.
     * @param self  IRI representing the self-reference of the state machine.
     */
    public ModelStateMachine(Model model, IRI self) {
        this.model = model;
        this.self = self;
        this.initialize();
    }

    /**
     * Hydrate the initial / current state of the state machine.
     *
     */
    public void initialize() {
        Iterator<Resource> found_initial = find(self, initialStep).iterator();
        if (found_initial.hasNext()) {
            setInitial(found_initial.next());
        }
        Iterator<Resource> found_current = find(self, hasCurrentState).iterator();
        if (found_current.hasNext()) {
            setCurrentState(found_current.next());
        }
        log.info("initialized: {} -> {} @ {}", initialState, getState(), self);
    }

    @Override
    public I_StateMachine<Resource> setInitial(Resource initialState) {
        model.remove(getSelf(), initialStep, null);
        model.add(getSelf(), initialStep, initialState);
        model.add(getSelf(), RDF.TYPE, A_WORKFLOW);
        return super.setInitial(initialState);
    }

    @Override
    public boolean isAllowed(Resource target) {
        if (this.currentState != null && this.currentState.equals(target)) return true;
        Iterable<Statement> transitions = model.getStatements(getState(), nextStep, target);

        boolean hasTransitions = transitions.iterator().hasNext();
        log.info("allowed.transition?: {} -> {} == {}", getState(), target, hasTransitions);
        if (!hasTransitions) return false; // No transitions

        log.info("allowed/final&guarded: {} -> {} & {}", isAllowedByGuard(self,target), isFinal(getState()), isGuarded(target));
//        if (isFinal(getState())) return false; // No states
        if (!isGuarded(target)) return true; // Not guarded
        return isAllowedByGuard(self, target); // Ask the guards ...
    }

    public boolean isGuarded(Resource state) {
        return !find(state, hasGuard).isEmpty();
    }

    /**
     * Check if the transition is allowed based on guard rules
     *
     * @param subject      The subject resource.
     * @param target  The target state resource.
     * @return true if the transition is allowed, false otherwise.
     */
    public boolean isAllowedByGuard(Resource subject, Resource target) {
        Collection<Resource> guards = find(target, hasGuard);
        Iterator<Resource> iGuards = guards.iterator();
        log.info("guards: {} -> {} -> {}", subject, target, iGuards.hasNext());
        if (!iGuards.hasNext()) return true; // No guards, we're good

        while (iGuards.hasNext()) {
            Resource guard = iGuards.next();
            Iterable<Statement> rules = model.getStatements(guard, null, null);
            Iterator<Statement> iRules = rules.iterator();
            log.info("guard: {} --> {} = {}", subject, guard, iRules.hasNext());

            // ensure the rule 2-tuple match the subject's 2-tuple (aka name/value)
            while (iRules.hasNext()) {
                Statement rule = iRules.next();
                log.info("check: {} --> {} = {}", subject, rule.getPredicate(), rule.getObject());
                if ( ! hasGuard.equals(rule.getPredicate()) ) {
                    // ensure rules tuples match the subject
                    Iterable<Statement> statements = model.getStatements(subject, rule.getPredicate(), rule.getObject());
                    boolean matches = (!rule.getPredicate().equals(hasGuard) && statements.iterator().hasNext());
                    log.info("guard.matched: {} -> {}", statements.iterator().hasNext(), matches);
                    if (!matches) return false;
                }
            }
        }
        log.info("guard.grants: {} -> {}", subject, target);
        return true; // All rules must have matched
    }

    /**
     * set the current state on the instance and in the model [idempotent]
     * @param target Resource
     */
    @Override
    public void setCurrentState(Resource target) {
        this.currentState = target;
        model.remove(self, hasCurrentState, null);
        model.add(self, hasCurrentState, target);
    }

    @Override
    protected Collection<Resource> getTransitions(Resource state) {
        // Get transitions for the given state
        Collection<Resource> transitions = find(state, nextStep);

        if (!isGuarded(state)) return transitions;

        // Filter transitions based on guard conditions
        return transitions.stream()
                .filter(transition -> isAllowedByGuard(self, transition))
                .collect(Collectors.toList());
    }

    /**
     * Adds a simple transition from one state to another.
     *
     * @param from The source state.
     * @param to   The target state.
     */
    public void add(Resource from, Resource to) {
        log.debug("add: [{}/{}] -> {} ==> {} == {}",model.size(), model.isEmpty(), from, to, getState());
        if (initialState==null) {
            this.setInitial(from);
            log.debug("initial: {} == {}", this.initialState, this.currentState);
        }
        model.add(from, nextStep, to);
    }

    /**
     * Adds a transition from one state to another and attaches a guard with the specified predicate/object tuple.
     *
     * @param from      The source state.
     * @param to        The target state.
     * @param predicate The predicate of the guard.
     * @param object    The object of the guard.
     */
    public void add(Resource from, Resource to, IRI predicate, Value object) {
        add(from, to);
        BNode guard = Values.bnode();
        model.add(to, hasGuard, guard);
        model.add(guard, predicate, object);
    }

    /**
     * Checks if a state is final, meaning it has no outgoing transitions.
     *
     * @param state The state to check.
     * @return True if the state is final (no transitions), false otherwise.
     */
    public boolean isFinal(Resource state) {
        return find(state, nextStep).isEmpty();
    }

    /**
     * Find resources related to a given subject with a specific predicate.
     *
     * @param state     The subject resource.
     * @param predicate The predicate IRI.
     * @return A collection of related resources.
     */
    private Collection<Resource> find(Resource state, IRI predicate) {
        Collection<Resource> found = new HashSet<>();

        for (Statement next : model.getStatements(state, predicate, null)) {
            if (next.getObject().isResource()) {
                found.add((Resource) next.getObject());
            }
        }
        return found;
    }

    @Override
    public IRI getSelf() {
        return self;
    }
}
