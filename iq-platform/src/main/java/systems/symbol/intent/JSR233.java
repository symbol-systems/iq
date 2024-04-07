package systems.symbol.intent;

import systems.symbol.agent.ScriptFacade;
import systems.symbol.annotation.RDF;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.AssetMimeTypes;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import javax.script.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An intent implementation that executes scripts using JSR-233 (Java Scripting API).
 * Extends the AbstractIntent class.
 */
public class JSR233 extends AbstractIntent {

ScriptEngineManager engineManager = new ScriptEngineManager();

/**
 * Constructs a new JSR233 intent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public JSR233(Model model, IRI self) {
super(model, self);
}

/**
 * Creates a new bindings object for script execution.
 *
 * @return A SimpleBindings object for script bindings.
 */
public SimpleBindings newBindings() {
SimpleBindings bindings = new SimpleBindings();
bindings.put("iq", new ScriptFacade(getModel(), getIdentity()));
return bindings;
}

/**
 * Executes the JSR-233 script based on the provided subject and resource.
 *
 * @param subject   The subject of the execution.
 * @param resource  The resource containing the script.
 * @return A set of IRIs indicating the completion of execution.
 */
@Override
@RDF(COMMONS.IQ_NS + "jsr-script")
public Set<IRI> execute(IRI subject, Resource resource) {
Set<IRI> done = new HashSet<>();
Literal script = ScriptCatalog.findScript(model, resource, null, null);
if (script == null) return done;
try {
SimpleBindings bindings = newBindings();
bindings.put("self", subject);
Object result = executeScript(script, bindings);
if (result == null) return done;
done.add(subject);
} catch (ScriptException e) {
throw new RuntimeException(e);
}
return done;
}

/**
 * Executes the script using the provided script engine and bindings.
 *
 * @param scriptThe script to execute.
 * @param bindings  The bindings for the script execution.
 * @return The result of the script execution.
 * @throws ScriptException If an error occurs during script execution.
 */
Object executeScript(Literal script, SimpleBindings bindings) throws ScriptException {
String mime = AssetMimeTypes.toMimeType(script.getDatatype());
if (mime == null) return null;
log.info("agent.script.execute: {} -> {}", script, mime);
ScriptEngine engine = engineManager.getEngineByMimeType(mime);
if (engine == null) {
return null;
}
engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
return engine.eval(script.stringValue());
}
}
