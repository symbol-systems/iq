package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.finder.I_FactFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Set;

/**
 * An intent
 */
public class Find implements I_Intent, I_Self {
    private final IRI self;
    private final I_FactFinder finder;
    private final Model model;
    private final IRI knows;

    /**
     * Constructs a new SPARQL intent with the provided Connection and self identity.
     *
     * @param self  The self identity of the agent.
     */
    public Find(IRI self, Model model, I_FactFinder finder) {
        this.self = self;
        this.finder = finder;
        this.model = model;
        this.knows = Values.iri(COMMONS.IQ_NS, "knows");
    }

    /**
     * Creates a new bindings object for script execution.
     * Executes the SPARQL query based on the provided actor and resource.
     *
     * @param actor   The actor of the execution.
     * @param state  The resource containing the script.
     * @param ctx        Bindings used in query interpolation.
     * @return A set of IRIs indicating the completion of execution.
     */
    @Override
    @RDF(COMMONS.IQ_NS + "find")
    public Set<IRI> execute(IRI actor, Resource state, Bindings ctx) throws StateException {
        Literal prompt = IQScripts.findScript(model, state, null, null);
        if (prompt==null||prompt.stringValue().isEmpty()) return new IRIs();
        try {
            Bindings bindings = MyFacade.rebind(actor, ctx);
            String query = HBSRenderer.template(prompt.stringValue(), bindings);
            Model found = finder.search(query);
            Set<IRI> iris = Models.subjectIRIs(found);
            for(IRI iri:iris) {
                model.add(actor, this.knows, iri);
            }
            return iris;
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }

    @Override
    public IRI getSelf() {
        return self;
    }
}
