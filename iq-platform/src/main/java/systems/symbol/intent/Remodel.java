/**
 * Package: systems.symbol.intent
 * Description: This package contains classes related to defining intents in IQ.
 *              Intents represent actions or commands to be executed by agents within the IQ system.
 *              Each intent is associated with a specific semantic meaning and is implemented as a subclass of AbstractIntent.
 *              Intents are defined using RDF triples and are executed based on the state of the system and input parameters.
 */

package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import systems.symbol.RDF;
import systems.symbol.agent.Facades;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings.FAIL_ON_INVALID_LINES;
import static systems.symbol.agent.Facades.SELF;

/**
 * Intent representing the act of remodeling RDF graph in IQ.
 *
 * This intent is responsible for dynamically generating RDF graph based on the
 * provided template and
 * the SPARQL results then adding them into the model.
 *
 * Remodeling RDF graph involves generating a new RDF graph by binding SPARQL
 * query results to templates
 * then rendering them using Handlebars.
 *
 * This class is designed to be instantiated with a self IRI, an RDF4J model
 * containing the knowledge
 * graph, and a contents provider for accessing template contents. By adhering
 * to the contract defined
 * in AbstractIntent, it seamlessly integrates with the IQ operating system and
 * enables agents to leverage
 * its capabilities for symbolic cognition.
 *
 * @see systems.symbol.intent.AbstractIntent
 * @see systems.symbol.RDF
 * @see systems.symbol.platform.IQ_NS
 */
public class Remodel extends AbstractIntent {

    IRI templateMime;
    I_Contents contents;

    /**
     * Constructor: Remodel
     * Description: Initializes a Remodel intent with the specified self IRI, model,
     * and contents provider.
     *
     * @param self     The IRI representing the self or identity of the intent.
     * @param model    The RDF4J model containing the knowledge graph.
     * @param contents The contents provider for accessing template contents.
     */
    public Remodel(IRI self, Model model, I_Contents contents) {
        boot(self, model);
        this.templateMime = null;
        this.contents = contents;
    }

    /**
     * Protected Constructor: Remodel
     * Description: Initializes a Remodel intent with the specified self IRI, model,
     * template MIME type, and contents provider.
     * This constructor is primarily used for testing and internal purposes.
     *
     * @param self         The IRI representing the self or identity of the intent.
     * @param model        The RDF4J model containing the knowledge graph.
     * @param templateMime The MIME type of the template.
     * @param contents     The contents provider for accessing template contents.
     */
    protected Remodel(IRI self, Model model, IRI templateMime, I_Contents contents) {
        boot(self, model);
        this.templateMime = templateMime;
        this.contents = contents;
    }

    /**
     * Method: remodel
     * Description: Binds SPARQL results as data and renders a single state into a
     * new Literal.
     *
     * @param actor    The actor source of models.
     * @param state    The state for each model.
     * @param bindings Bindings containing additional parameters for the execution.
     * @return Set of one IRI for the new triple.
     * @throws IOException if an I/O error occurs while performing the remodeling
     *                     operation.
     */
    public Set<IRI> remodel(IRI actor, Resource state, Bindings bindings) throws IOException {
        Set<IRI> done = new HashSet<>();
        Literal hbs = contents.getContent(state, templateMime);
        log.info("remodel: {} -> {} - {}", actor, state, hbs != null);
        if (hbs == null)
            return done;
        done.addAll(remodel(actor, state, hbs, bindings, model));
        return done;
    }

    /**
     * Method: safeCast
     * Description: Safely casts raw results to a list of maps.
     *
     * @param rawResults The raw results to cast.
     * @return A list of maps if the cast is successful, otherwise null.
     */
    public static List<Map<String, Object>> safeCast(Object rawResults) {
        if (rawResults == null)
            return null;
        if (!(rawResults instanceof List))
            return null;
        List<?> rawList = (List<?>) rawResults;
        if (rawList.isEmpty())
            return new ArrayList<>();
        if (rawList.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) rawResults;
            return results;
        }
        return null;
    }

    /**
     * Description: Remodels RDF data based on the provided template and bindings.
     * The template uses Handlebars to interpolate RDF
     *
     * @param actor     The actor/agent who is executing the intent.
     * @param state     The current state of the actor/agent.
     * @param rdfString The RDF data as a literal string.
     * @param my        Additional bindings for the execution.
     * @param model     The RDF4J model containing the knowledge graph.
     * @return The set of learned facts.
     * @throws IOException if an I/O error occurs while performing the remodeling
     *                     operation.
     */
    public Set<IRI> remodel(IRI actor, Resource state, Literal rdfString, Bindings my, Model model) throws IOException {
        Bindings bindings = Facades.rebind(actor, state, my);
        log.info("remodel.rdf: {} -> {} -> {}", rdfString.getDatatype(), bindings.keySet(),
                ((Map<?, ?>) bindings.get(Facades.MY)).keySet());

        String remodelled = HBSRenderer.template(rdfString.stringValue(), bindings);

        String mime = FileFormats.toMime(templateMime);
        RDFFormat format = Rio.getWriterFormatForMIMEType(mime).orElseGet(() -> RDFFormat.TURTLE);
        log.info("remodel.format: {} --> {}", actor, format);

        ParserConfig config = new ParserConfig();
        config.set(FAIL_ON_INVALID_LINES, false);

        String iri = my.containsKey(SELF) ? my.get(SELF).toString() : actor.stringValue();
        try {
            Model parsed = Rio.parse(new StringReader(remodelled), iri, format);
            model.addAll(parsed);
            return Models.subjectIRIs(parsed);
        } catch (RDFParseException e) {
            log.error("remodel.failed: {} ", remodelled, e);
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Method: execute
     * Description: Executes the remodeling intent.
     *
     * @param actor    The actor/agent who is executing the intent.
     * @param state    The current state of the actor/agent.
     * @param bindings Additional bindings for the execution.
     * @return The set of learned facts.
     * @throws StateException if an error occurs while executing the intent.
     */
    @Override
    @RDF(IQ_NS.IQ + "remodel")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        try {
            return remodel(actor, state, bindings);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state, e);
        }
    }
}
