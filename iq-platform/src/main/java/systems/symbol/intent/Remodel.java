package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.rdf4j.store.I_Contents;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings.FAIL_ON_INVALID_LINES;
import static systems.symbol.agent.MyFacade.INTENT;

public class Remodel extends AbstractIntent {

    IRI templateMime;
    I_Contents contents;

    public Remodel(IRI self, Model model, I_Contents contents) {
        super(model, self);
        this.templateMime = null;
        this.contents = contents;
    }

    protected Remodel(IRI self, Model model, IRI templateMime, I_Contents contents) {
        super(model, self);
        this.templateMime = templateMime;
        this.contents = contents;
    }

    /**
     * Bind SPARQL results as data and render a single state into a new Literal
     *
     * @param actor       actor source of models
     * @param state      state for each model
     * @return Set of one IRI for the new triple
     */
    public Set<IRI> remodel(IRI actor, Resource state, Bindings bindings) throws IOException {
        Set<IRI> done = new HashSet<>();
        Literal hbs = contents.getContent(state, templateMime);
        log.info("remodel: {} -> {} - {}", actor, state, hbs!=null);
        if (hbs == null) return done;
        done.addAll( remodel(actor, state, hbs, bindings, model) );
       return done;
    }

    public static List<Map<String, Object>> safeCast(Object rawResults) {
        if (rawResults == null ) return null;
        if (!(rawResults instanceof List)) return null;
        List<?> rawList = (List<?>) rawResults;
        if (rawList.isEmpty()) return new ArrayList<>();
        if (rawList.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) rawResults;
            return results;
        }
        return null;
    }

    protected Set<IRI> remodel(IRI actor, Resource state, Literal hbs, Bindings my, Model model) throws IOException {
        Bindings bindings = MyFacade.rebind(actor, state, my);
        log.info("remodel.bindings: {} -> {} -> {}", hbs.getDatatype(), bindings.keySet(), ((Map<?,?>)bindings.get("my")).keySet());
        String remodelled = HBSRenderer.template(hbs.stringValue(), bindings);

        String mime = FileFormats.toMime(templateMime);
        RDFFormat format = Rio.getWriterFormatForMIMEType(mime).orElseGet(() -> RDFFormat.TURTLE);
        log.info("remodel.format: {} --> {}", actor, format);

        ParserConfig config = new ParserConfig();
        config.set(FAIL_ON_INVALID_LINES, false);

        String intent = my.containsKey(INTENT)?my.get(INTENT).toString():actor.stringValue();
        Model parsed = Rio.parse(new StringReader(remodelled), intent, format);
        model.addAll(parsed);
        return Models.subjectIRIs(parsed);
    }

    @Override
    @RDF(COMMONS.IQ_NS+"remodel")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        try {
            return remodel(actor, state, bindings);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }
}
