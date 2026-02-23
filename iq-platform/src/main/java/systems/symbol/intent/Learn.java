/**
 * Intent representing the act of learning facts in IQ.
 *
 * Learning facts is a fundamental process in cognitive systems, allowing agents to
 * acquire new knowledge and update their internal knowledge representation. This intent
 * is responsible for adding knowledge represented by the "knows" predicate to an agent's
 * working memory, signifying that the agent has learned the specified fact.
 *
 * This intent is implemented is associated with the RDF triple pattern IQ:learn.
 * When executed, it adds the "knows" predicate to the actor for the specified fact.
 *
 * This class is designed to be instantiated with a well-known IRI and an RDF4J model containing
 * the knowledge graph. By adhering to the contract defined in AbstractIntent, it seamlessly
 * integrates with the IQ operating system and enables agents to leverage its capabilities
 * for symbolic cognition.
 *
 * @see systems.symbol.AbstractIntent
 * @see systems.symbol.RDF
 * @see systems.symbol.platform.IQ_NS
 */

package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static systems.symbol.platform.IQ_NS.KNOWS;

/**
 * Class: Learn
 * Description: Represents an intent for learning facts in IQ.
 *              A "learn" intent is used to add knowledge represented by the "knows" predicate to an agent's working memory.
 *              When executed, it adds the "knows" predicate to the actor for the specified fact.
 */
public class Learn extends AbstractIntent {

    /**
     * Constructor: Learn
     * Description: Initializes a Learn intent with the specified self IRI and model.
     *
     * @param self  The IRI representing the self or identity of the intent.
     * @param model The RDF4J model containing the knowledge graph.
     */
    public Learn(IRI self, Model model) {
        boot(self, model);
    }

    /**
     * Method: learn
     * Description: Adds the "knows" predicate to the actor for the specified fact.
     *
     * @param actor    The actor/agent who is learning.
     * @param fact     The fact that the actor should 'learn'.
     * @param _unused  Unused parameter for script bindings.
     * @return The set of learned facts (1 or 0).
     * @throws IOException if an I/O error occurs while performing the learn operation.
     */
    public Set<IRI> learn(IRI actor, Resource fact, Bindings _unused) throws IOException {
        Set<IRI> done = new HashSet<>();

        if (fact.isIRI()) {
            log.info("learn: {} -> {}", actor, fact);
            model.add(actor, KNOWS, fact, getSelf());
            done.add((IRI) fact);
        } else if (fact.isResource() ){
            IRI iri = Values.iri(IdentityHelper.uuid());
            Iterable<Statement> statements = getModel().getStatements(fact, null, null);
            for(Statement statement : statements){
                model.add(iri, statement.getPredicate(), statement.getObject(), getSelf());
                log.info("learn.#: {} -> {} -> {}", actor, iri, statement.getObject());
            }
            model.add(actor, KNOWS, iri, getSelf());
        } else if (fact.isLiteral() ){
            IRI iri = Values.iri(IdentityHelper.uuid());
            model.add(actor, KNOWS, iri, getSelf());
            model.add(iri, org.eclipse.rdf4j.model.vocabulary.RDF.VALUE, fact, getSelf());
            log.info("learn.$: {} -> {} -> {}", actor, iri, fact.stringValue());
        }
        return done;
    }

    /**
     * Method: execute
     * Description: Executes the Learn intent for the specified actor and state.
     *
     * @param actor     The actor/agent who is executing the intent.
     * @param state     The current state of the actor/agent.
     * @param bindings  Bindings containing additional parameters for the execution.
     * @return The set of learned facts.
     * @throws StateException if an error occurs while executing the intent.
     */
    @Override
    @RDF(IQ_NS.IQ + "learn")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        try {
            return learn(actor, state, bindings);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }
}
