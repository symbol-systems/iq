package systems.symbol.fsm;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_Self;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

import static systems.symbol.platform.IQ_NS.*;

/**
 * State machine (FSM) uses RDF4J Model for state representation.
 * RDF4J's Model to store information about thoughts, transitions, and guards.
 * The class initializes with a specified RDF model and self-reference IRI
 * the initial state is set to the first transition added
 * checks if transitions are allowed based on defined guard rules.
 *
 */
public class ModelStateMachine extends AbstractStateMachine<Resource> implements I_Self {
private final Model ground, thoughts;
protected IRI self;

/**
 * A finite state machine backed by an RDF4J Model to store information about
 * thoughts, transitions, and guards.
 * The class initializes with a specified RDF model and self-reference IRI,
 * extracting the initial state.
 * Checks if transitions are allowed based on defined guards.
 * Guards are "exemplars" meaning their predicates/objects must be matched by
 * `self` statements
 *
 * @param model RDF4J Model containing state machine information.
 * @param self  IRI representing the self-reference of the state machine.
 * @throws StateException
 */
public ModelStateMachine(IRI self, Model model) throws StateException {
this.self = self;
this.ground = model;
this.thoughts = model;
this.initialize();
}

public ModelStateMachine(IRI self, Model model, Model thoughts) throws StateException {
this.self = self;
this.ground = model;
this.thoughts = thoughts;
this.initialize();
}

@Override
public void initialize() throws StateException {
Resource found_initial = findCurrentState(hasInitialState);
if (found_initial == null)
found_initial = findGroundState(hasInitialState);
if (found_initial == null)
throw new StateException("oops.msm.initial", self);
setInitial(found_initial);
Resource current = findCurrentState(hasCurrentState);
if (current == null)
current = getInitial();
setCurrentState(current);
log.info("msm.initialized: {} == {}", current, getState());
}

protected void sync() {
Resource current = getState();
thoughts.remove(self, hasCurrentState, null);
if (current != null)
thoughts.add(self, hasCurrentState, current);

thoughts.remove(self, hasInitialState, null);
thoughts.add(self, hasInitialState, initialState);
log.info("msm.sync: {} @ {} -> {}", self, getState(), getTransitions(getState()));
}

@Override
public boolean isAllowed(Resource target) {
if (this.currentState != null && this.currentState.equals(target))
return true;
Iterable<Statement> transitions = ground.getStatements(getState(), TO, target);

boolean hasTransitions = transitions.iterator().hasNext();
if (!hasTransitions) {
log.debug("msm.allowed.transition?: {} -> {} == {}", getState(), target, hasTransitions);
return false; // No transitions
}

log.debug("msm.updated (final/guarded): {} ---> {} / {}", isAllowedByGuard(self, target), isFinal(getState()),
isGuarded(target));
// if (isFinal(getState())) return false; // No thoughts
if (!isGuarded(target))
return true; // Not guarded
return isAllowedByGuard(self, target); // Ask the guards ...
}

public boolean isGuarded(Resource state) {
return !find(ground, state, hasGuard).isEmpty();
}

/**
 * Check if the transition is allowed based on guard rules
 *
 * @param subject The subject resource.
 * @param target  The target state resource.
 * @return true if the transition is allowed, false otherwise.
 */
public boolean isAllowedByGuard(Resource subject, Resource target) {
Collection<Resource> guards = find(ground, target, hasGuard);
Iterator<Resource> iGuards = guards.iterator();
log.debug("msm.guarded?: {} @ {} -> {}", iGuards.hasNext(), subject, target);
if (!iGuards.hasNext())
return true; // No guards, we're good

while (iGuards.hasNext()) {
Resource guard = iGuards.next();
Iterable<Statement> rules = ground.getStatements(guard, null, null);
Iterator<Statement> iRules = rules.iterator();
log.debug("msm.guard: {} --> {} = {}", subject, guard, iRules.hasNext());

// ensure the rule 2-tuple match the subject's 2-tuple (aka name/value)
while (iRules.hasNext()) {
Statement rule = iRules.next();
log.debug("msm.guard.rule: {} --> {} = {}", subject, rule.getPredicate(), rule.getObject());
if (!hasGuard.equals(rule.getPredicate())) {
// ensure rules tuples match the subject
Iterable<Statement> statements = ground.getStatements(subject, rule.getPredicate(),
rule.getObject());
boolean matches = statements.iterator().hasNext();
if (!matches) {
log.debug("msm.guard.block: {} == {}", subject, rule.getPredicate());
return false;
}
}
}
}
log.debug("msm.guard.grants: {} -> {}", subject, target);
return true; // All rules must have matched
}

/**
 * set the current state on the instance and in the model [idempotent]
 * 
 * @param target Resource
 */
@Override
public void setCurrentState(Resource target) {
if (target == null)
return;
boolean initialized = getState() != null;
super.setCurrentState(target);
sync();
log.info("msm.current: {} : {} -> {}", initialized, target, getState());
}

@Override
protected Collection<Resource> getTransitions(Resource state) {
Collection<Resource> transitions = find(ground, state, TO);
if (!isGuarded(state))
return transitions;

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
log.debug("msm.add: [{}/{}] -> {} ==> {} == {}", ground.size(), ground.isEmpty(), from, to, getState());
if (initialState == null) {
this.setInitial(from);
log.debug("msm.initial: {} == {}", this.initialState, this.currentState);
}
ground.add(from, TO, to);
}

/**
 * Adds a transition from one state to another and attaches a guard with the
 * specified predicate/object tuple.
 *
 * @param from  The source state.
 * @param toThe target state.
 * @param predicate The predicate of the guard.
 * @param objectThe object of the guard.
 */
public void add(Resource from, Resource to, IRI predicate, Value object) {
add(from, to);
BNode guard = Values.bnode();
ground.add(to, hasGuard, guard);
ground.add(guard, predicate, object);
}

/**
 * Checks if a state is final, meaning it has no outgoing transitions.
 *
 * @param state The state to check.
 * @return True if the state is final (no transitions), false otherwise.
 */
public boolean isFinal(Resource state) {
return find(ground, state, TO).isEmpty();
}

/**
 * Find resources related to a given subject with a specific predicate.
 *
 * @param thoughts  The subject resource.
 * @param model
 * @param predicate The predicate IRI.
 * @return A collection of related resources.
 */
private Collection<Resource> find(Model model, Resource target, IRI predicate) {
Collection<Resource> found = new HashSet<>();

for (Statement next : model.getStatements(target, predicate, null)) {
if (next.getObject().isResource()) {
found.add((Resource) next.getObject());
}
}
return found;
}

public Resource findGroundState(IRI predicate) {
Iterator<Resource> found = find(ground, self, predicate).iterator();
if (found.hasNext()) {
return found.next();
}
return null;
}

public Resource findCurrentState(IRI predicate) {
Iterator<Resource> found = find(thoughts, self, predicate).iterator();
if (found.hasNext()) {
return found.next();
}
return null;
}

@Override
public IRI getSelf() {
return self;
}

public Model getGround() {
return ground;
}

public Model getThoughts() {
return thoughts;
}
}
