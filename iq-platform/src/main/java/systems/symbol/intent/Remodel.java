package systems.symbol.intent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import systems.symbol.agent.MyFacade;
import systems.symbol.annotation.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Remodel extends AbstractIntent {

private ScriptCatalog catalog;
IRI templateMime;
RepositoryConnection connection;
public static final IRI sparqlMime = Values.iri("urn:" + COMMONS.MIME_SPARQL);
public static final IRI hbsMime = Values.iri("urn:" + COMMONS.MIME_HBS);
public static final IRI ttlMime = Values.iri("urn:" + RDFFormat.TURTLE.getName());

protected Remodel(RepositoryConnection connection, Model model, IRI self) {
super(model, self);
this.connection = connection;
this.templateMime = null;
this.catalog = new ScriptCatalog(self, connection);
}

protected Remodel(RepositoryConnection connection, Model model, IRI self, IRI templateMime) {
super(model, self);
this.connection = connection;
this.templateMime = templateMime;
}

/**
 * Bind SPARQL results as data and render a single state into a new Literal
 *
 * @param actor   actor source of models
 * @param state  state for each model
 * @return Set of one IRI for the new triple
 */
public Set<IRI> remodel(IRI actor, Resource state, Bindings bindings) throws IOException {
Set<IRI> done = new HashSet<>();

Literal hbs = catalog.getContent(state, templateMime);
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

protected Set<IRI> remodel(IRI actor, Resource state, Literal hbs, Map<String,Object> my, Model model) throws IOException {
Bindings bindings = MyFacade.rebind(actor, state, my);
log.info("remodel.bindings: {} -> {}", hbs.getDatatype(), bindings.keySet());
String remodelled = HBSRenderer.template(hbs.stringValue(), bindings);
log.info("remodelled: {} ->\n{}", actor, remodelled);
//RDFFormat format = RDFFormat.
Model parsed = Rio.parse(new StringReader(remodelled), actor.stringValue(), RDFFormat.TURTLE);
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
