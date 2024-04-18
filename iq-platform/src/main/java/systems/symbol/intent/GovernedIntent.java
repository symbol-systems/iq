package systems.symbol.intent;

import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.model.I_Self;
import systems.symbol.COMMONS;

import javax.script.Bindings;
import java.util.Set;

import static systems.symbol.platform.Provenance.generated;

/**
 * The GovernedIntent class represents an intent that is governed by a set of rules or policies.
 * It implements the I_Intent interface and introduces governance by adding provenance information
 * using the Provenance class for each executed intent.
 */
public class GovernedIntent implements I_Intent, I_Self {

    private final IRI self;
    private final Model model;
    private final I_Intent intent;

    /**
     * Constructs an instance of GovernedIntent.
     *
     * @param model  The RDF model used for storing provenance information.
     * @param self   The IRI representing the identity of this GovernedIntent instance.
     * @param intent The underlying intent that is being governed.
     */
    public GovernedIntent(IRI self, Model model, I_Intent intent) {
        this.model = model;
        this.self = self;
        this.intent = intent;
    }

    /**
     * Executes the governed intent and adds provenance information for each executed intent result.
     *
     * @param actor   The IRI representing the entity involved in the intent execution.
     * @param state The IRI representing the activity associated with the intent execution.
     * @return A set of IRIs representing the results of the executed intent.
     */
    @Override
    @RDF(COMMONS.IQ_NS+"govern")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        Set<IRI> done = intent.execute(actor, state, bindings);

        // Add provenance information for each intent result
        done.forEach(result -> {
            generated(model, actor, state, result, getSelf());
        });

        return done;
    }

    /**
     * Gets the identity (IRI) of this GovernedIntent instance.
     *
     * @return The IRI representing the identity of this GovernedIntent instance.
     */
    @Override
    public IRI getSelf() {
        return self;
    }
}

