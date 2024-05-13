package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static systems.symbol.platform.IQ_NS.FORGOT;
import static systems.symbol.platform.IQ_NS.KNOWS;

/**
 * Intent representing the act of forgetting facts in IQ.
 *
 * Forgetting facts is a critical aspect of cognitive systems, enabling agents to update
 * their internal knowledge representation by removing outdated or irrelevant information.
 * This intent is responsible for removing knowledge represented by the "knows" predicate
 * from an agent's working memory and adding the "forgot" predicate to signify that the
 * knowledge has been forgotten. The operation is logically isomorphic and idempotent,
 * meaning that the semantic of the graph remains the same, but the operation is performed
 * via a different predicate path in the graph.
 *
 * When executed, it removes the "knows" predicate from the actor and adds the "forgot".
 *
 * This class is instantiated with a well-known self IRI and an RDF4J model containing
 * the knowledge graph. By adhering to the contract defined in AbstractIntent, it seamlessly
 * integrates with the IQ operating system and enables agents to leverage its capabilities
 * for symbolic cognition.
 *
 * @see systems.symbol.intent.AbstractIntent
 * @see systems.symbol.RDF
 * @see systems.symbol.platform.IQ_NS
 */

public class Forget extends AbstractIntent {
    public Forget(IRI self, Model model) {
        boot(self, model);
    }

    /**
     * A `forget` is a fact and removes the `knows` predicate from the actor, then adds `forgot`.
     * The operation is logically isomorphic and idempotent - the semantic of the graph are the same.
     * That said, it's via different predicate path in the graph.
     *
     * @param actor     The actor/agent who is forgetting
     * @param fact      The fact that the actor should  'forget'
     * @return The set of forgotten facts (1 or 0)
     */
    public Set<IRI> forget(IRI actor, Resource fact, Bindings _unused) throws IOException {
        Set<IRI> done = new HashSet<>();
        model.remove(actor, KNOWS, fact);
        model.add(actor, FORGOT, fact);
        if (fact instanceof IRI) done.add((IRI)fact);
        return done;
    }


    @Override
    @RDF(IQ_NS.IQ+"forget")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        try {
            return forget(actor, state, bindings);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }

}
