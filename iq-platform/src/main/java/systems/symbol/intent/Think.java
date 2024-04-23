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
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.Prompts;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.render.HBSRenderer;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

import static org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings.FAIL_ON_INVALID_LINES;
import static systems.symbol.agent.MyFacade.INTENT;

public class Think extends AbstractIntent {

IRI templateMime;
I_Contents contents;
I_LLM<String> llm;
Pattern extractBody = Pattern.compile("```(\\w+\n)\\s*(.*?)\n```", Pattern.DOTALL);

public Think(IRI self, Model model, I_Contents contents, I_LLM<String> llm) {
boot(self, model);
this.templateMime = null;
this.contents = contents;
this.llm = llm;
}

protected Think(IRI self, Model model, IRI templateMime, I_Contents contents, I_LLM<String> llm) {
boot(self, model);
this.templateMime = templateMime;
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
Literal hbs = contents.getContent(state, templateMime);
log.info("thinks: {} @ {} - {}", actor, state, hbs!=null);
if (hbs == null) return done;
done.addAll( thinks(actor, state, hbs, bindings, model) );
   return done;
}

protected Set<IRI> thinks(IRI actor, Resource state, Literal hbs, Bindings my, Model model) throws IOException, APIException {
Bindings bindings = MyFacade.rebind(actor, state, my);
String intent = my.containsKey(INTENT)?my.get(INTENT).toString():IdentityHelper.uuid(actor.stringValue()+"#");

log.info("think.bindings: {} -> {} -> {}", hbs.getDatatype(), bindings.keySet(), ((Map<?,?>)bindings.get("my")).keySet());
String mime = FileFormats.toMime(templateMime);
RDFFormat format = Rio.getWriterFormatForMIMEType(mime).orElseGet(() -> RDFFormat.TURTLE);

String remodelled = HBSRenderer.template(hbs.stringValue(), bindings);
log.info("think.raw: {} -> {}", format, remodelled);

I_Thread<String> thought = Prompts.think(actor, state, intent, model, my, format);
thought.user(remodelled);
log.info("think.prompt: {}", thought);
llm.complete(thought);
String rdf = hackItToWork(thought.latest().getContent());
log.info("think.thoughts: {}", rdf);

ParserConfig config = new ParserConfig();
config.set(FAIL_ON_INVALID_LINES, false);

Model parsed = Rio.parse(new StringReader(rdf), intent, format);
model.addAll(parsed);
return Models.subjectIRIs(parsed);
}

String hackItToWork(String msg) {
Matcher matcher = extractBody.matcher(msg);
if (!matcher.find()) return msg;
return matcher.group(2);
}

@Override
@RDF(COMMONS.IQ_NS+"think")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
try {
return thinks(actor, state, bindings);
} catch (IOException | APIException e) {
throw new StateException(e.getMessage(), state, e);
}
}
}
