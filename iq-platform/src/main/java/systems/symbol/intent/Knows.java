package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import systems.symbol.agent.MyFacade;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Knows extends AbstractIntent {

    public final static IRI KNOWS = Values.iri(COMMONS.IQ_NS, "knows");
    public Knows(IRI self, Model model) {
        super(model, self);
    }

    /**
     * Bind SPARQL results as data and render a single state into a new Literal
     *
     * @param actor       actor source of models
     * @param state      state for each model
     * @return Set of one IRI for the new triple
     */
    public Set<IRI> knows(IRI actor, Resource state, Bindings _unused) throws IOException {
        Set<IRI> done = new HashSet<>();

        log.info("knows: {} -> {}", actor, state);
        model.add(actor, KNOWS, state, getSelf());
        if (state instanceof IRI) done.add((IRI)state);
        return done;
    }


    @Override
    @RDF(COMMONS.IQ_NS+"knows")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        try {
            return knows(actor, state, bindings);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }

}
