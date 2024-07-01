package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.finder.I_ModelFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Set;

import static systems.symbol.platform.IQ_NS.KNOWS;

/**
 * An intent implementation that executes scripts using SPARQL (Java Scripting API).
 * Extends the AbstractIntent class.
 */
public class Search extends AbstractIntent {
    private final I_ModelFinder finder;
    private final Model ground;

    /**
     * Constructs a new SPARQL intent with the provided Connection and self identity.
     *
     * @param self  The self identity of the agent.
     */
    public Search(IRI self, Model model, I_ModelFinder finder, Model ground) {
        boot(self, model);
        this.finder = finder;
        this.ground = ground;
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
    @RDF(IQ_NS.IQ + "search")
    public Set<IRI> execute(IRI actor, Resource state, Bindings ctx) throws StateException {
        Literal prompt = IQScripts.findScript(model, state, null, null);
        if (prompt==null || prompt.stringValue().isEmpty()) return new IRIs();
        try {
            Bindings bindings = MyFacade.rebind(actor, ctx);
            String query = HBSRenderer.template(prompt.stringValue(), bindings);
            Model found = finder.find(query);
            Set<IRI> iris = Models.subjectIRIs(found);
            for(IRI iri:iris) {
                learn(actor, iri);
            }
            return iris;
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }

    private void learn(IRI actor, IRI fact) {
        Iterable<Statement> facts = ground.getStatements(fact, null, null);
        facts.forEach( (f) -> {
            model.add(f);
            model.add(actor, KNOWS, f.getSubject());
        });
        model.add(actor, KNOWS, fact);
    }

    @Override
    public IRI getSelf() {
        return self;
    }
}
