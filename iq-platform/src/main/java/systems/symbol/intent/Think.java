package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.render.HBSRenderer;
import systems.symbol.string.Extract;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import static org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings.FAIL_ON_INVALID_LINES;
import static systems.symbol.agent.MyFacade.SELF;

/**
 * An intent that processes and renders structured content into a format suitable for human consumption.
 * It takes a template in Handlebars format and binds it with provided data, rendering the content.
 * The rendered content is then passed to a conversational agent for further processing.
 */

public class Think extends AbstractIntent {

IRI fallbackMime;
I_Contents contents;
I_LLM<String> llm;

public Think(IRI self, Model model, I_Contents contents, I_LLM<String> llm) {
boot(self, model);
this.fallbackMime = null;
this.contents = contents;
this.llm = llm;
}

protected Think(IRI self, Model model, IRI fallbackMime, I_Contents contents, I_LLM<String> llm) {
boot(self, model);
this.fallbackMime = fallbackMime;
this.contents = contents;
this.llm = llm;
}

/**
 * Bind SPARQL results as data and render a single state into a new Literal
 *
 * @param actor   actor source of models
 * @param state  state for each model
 * @return Set of one IRI for the new triple
 */
public Set<IRI> thinks(IRI actor, Resource state, Bindings bindings) throws IOException, APIException {
Set<IRI> done = new HashSet<>();
Literal hbs = contents.getContent(state, fallbackMime);
log.info("thinks: {} @ {} - {}", actor, state, hbs!=null);
if (hbs == null) return done;
done.addAll( thinks(actor, state, hbs, bindings, model) );
   return done;
}

/**
 * Interpolates the template, sending it to an LLM for neural inference, then merges the new graph into the existing model.
 *
 * The LLM is expected to return valid RDF in the expected format - either the mimetype, the .
 *
 * The format of the returned RDF is inferred from the datatype of the template.
 *
 * @param actor   The actor/source of the models.
 * @param state   The state for each model.
 * @param template The Handlebars template to render.
 * @param my  Bindings for script execution.
 * @param model   The RDF4J model associated with the intent.
 * @return A set of IRIs representing the result of the execution.
 * @throws IOExceptionIf an IO exception occurs.
 * @throws APIException   If an API exception occurs.
 */

protected Set<IRI> thinks(IRI actor, Resource state, Literal template, Bindings my, Model model) throws IOException, APIException {
Bindings bindings = MyFacade.rebind(actor, state, my);
String intent = my.containsKey(SELF)?my.get(SELF).toString():IdentityHelper.uuid(actor.stringValue()+"#");

log.info("think.bindings: {} -> {}", template.getDatatype(), bindings.keySet());
String mime = FileFormats.toMime(fallbackMime);

// Determine the RDF format based on the datatype of the template ***REMOVED***, or TURTLE.
RDFFormat format = Rio.getWriterFormatForMIMEType(template.getDatatype().stringValue())
.orElseGet(() -> Rio.getWriterFormatForMIMEType(mime).orElse(RDFFormat.TURTLE));

String remodelled = HBSRenderer.template(template.stringValue(), bindings);
log.info("think.raw: {} -> {}", format, remodelled);

I_Assist<String> thought = null;// Prompts.think(actor, state, intent, model, my, format);
thought.user(remodelled);
log.info("think.prompt: {}", thought);
llm.complete(thought);
String rdf = Extract.hackItToWork(thought.latest().getContent());
log.info("think.thoughts: {}", rdf);

ParserConfig config = new ParserConfig();
config.set(FAIL_ON_INVALID_LINES, false);

Model parsed = Rio.parse(new StringReader(rdf), intent, format);
model.addAll(parsed);
return Models.subjectIRIs(parsed);
}

@Override
@RDF(IQ_NS.IQ+"think")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return thinks(actor, state, bindings);
} catch (IOException | APIException e) {
throw new StateException(e.getMessage(), state, e);
}
}
}
