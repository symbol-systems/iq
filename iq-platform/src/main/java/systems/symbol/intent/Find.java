package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.RDF;
import systems.symbol.agent.Facades;
import systems.symbol.finder.I_ModelFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Set;

/**
 * An intent that semantically finds facts within IQ.
 * *
 * This intent enables agents to search for facts based on a semantic query. It
 * utilizes a fact finder
 * to perform the search and returns the found facts as a set of IRIs.
 * *
 * The intent is instantiated with a well-known IRI, an RDF4J model containing
 * the knowledge graph,
 * and a fact finder implementation.
 * *
 * By adhering to the contract defined in I_Intent and I_Self, it seamlessly
 * integrates with the IQ operating
 * system and enables agents to leverage its capabilities for symbolic
 * cognition.
 *
 * @see systems.symbol.intent.I_Intent
 * @see systems.symbol.platform.I_Self
 * @see I_ModelFinder
 */
public class Find implements I_Intent, I_Self {
    private final IRI self;
    private final I_ModelFinder finder;
    private final Model model;
    private final IRI knows;

    /**
     * Constructs a new Find intent with the provided self identity, RDF4J model,
     * and fact finder.
     *
     * @param self   The self identity of the agent.
     * @param model  The RDF4J model associated with the agent.
     * @param finder The fact finder implementation used for searching.
     */
    public Find(IRI self, Model model, I_ModelFinder finder) {
        this.self = self;
        this.finder = finder;
        this.model = model;
        this.knows = Values.iri(IQ_NS.IQ, "knows");
    }

    /**
     * Executes the Find intent, searching for facts based on the provided semantic
     * query.
     *
     * @param actor  The actor executing the intent.
     * @param intent The resource containing the semantic query.
     * @param ctx    Additional bindings for query interpolation.
     * @return A set of IRIs representing the found facts.
     * @throws StateException If an error occurs during the execution of the intent.
     */
    @Override
    @RDF(IQ_NS.IQ + "find")
    public Set<IRI> execute(IRI actor, Resource intent, Bindings ctx) throws StateException {
        Literal prompt = IQScripts.findScript(model, intent, null, null);
        if (prompt == null || prompt.stringValue().isEmpty())
            return new IRIs();
        try {
            Bindings bindings = Facades.rebind(actor, ctx);
            String query = HBSRenderer.template(prompt.stringValue(), bindings);
            Model found = finder.find(query);
            Set<IRI> iris = Models.subjectIRIs(found);
            for (IRI iri : iris) {
                model.add(actor, this.knows, iri);
            }
            return iris;
        } catch (IOException e) {
            throw new StateException(e.getMessage(), intent, e);
        }
    }

    /**
     * Retrieves the self identity associated with this intent.
     *
     * @return The self identity of the intent.
     */
    @Override
    public IRI getSelf() {
        return self;
    }
}
