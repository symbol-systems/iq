package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.rdf4j.util.SupportedScripts;
import systems.symbol.secrets.I_Secrets;

import javax.script.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An intent implementation that executes scripts using JSR-233 (Java Scripting API).
 * Extends the AbstractIntent class.
 */
public class JSR233 extends AbstractIntent {

ScriptEngineManager engineManager = new ScriptEngineManager();
I_Secrets secrets;

/**
 * Constructs a new JSR233 intent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public JSR233(IRI self, Model model) {
super(model, self);
}

/**
 * Constructs a new JSR233 intent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public JSR233(IRI self, Model model, I_Secrets secrets) {
super(model, self);
this.secrets = secrets;
}
/**
 * Executes the JSR-233 script based on the provided actor and resource.
 *
 * @param actor   The actor of the execution.
 * @param state  The resource containing the script.
 * @return A set of IRIs indicating the completion of execution.
 */
@Override
@RDF(COMMONS.IQ_NS + "script")
public Set<IRI> execute(IRI actor, Resource state, Bindings my) throws StateException {
Set<IRI> done = new HashSet<>();
Literal script = IQScripts.findScript(model, state, null, null);
if (script == null) return done;
try {
Object result = executeScript(script, actor, state, my );
done.add(actor);
//log.info("script.result: {} -> {}", state, result);
} catch (ScriptException e) {
throw new RuntimeException(e);
}
return done;
}

/**
 * Executes the script using the provided script engine and bindings.
 *
 * @param script The script to execute.
 * @param state The state that trigger the intent
 * @param my The bindings for script runtime.
 * @return The result of the script execution.
 * @throws ScriptException If an error occurs during script execution.
 */
Object executeScript(Literal script, IRI actor, Resource state, Bindings my) throws ScriptException {
String mime = SupportedScripts.toMimeType(script.getDatatype());
//log.info("script.execute: {} -> {}", script, mime);
if (mime == null) return null;
ScriptEngine engine = engineManager.getEngineByMimeType(mime);
if (engine == null) {
return null;
}
engine.setBindings( MyFacade.rebind(actor, state, getModel(), my, secrets), ScriptContext.ENGINE_SCOPE);
return engine.eval(script.stringValue());
}
}
