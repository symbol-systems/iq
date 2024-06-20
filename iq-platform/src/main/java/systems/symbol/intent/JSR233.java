package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.RDF;
import systems.symbol.agent.MyFacade;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.sparql.IQScripts;
import systems.symbol.rdf4j.util.SupportedScripts;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An intent that executes scripts using JSR-233 - the Java Scripting API.
 *
 * This class represents an intent capable of executing scripts written in various scripting languages supported
 * by the JSR-233 (Java Scripting API) standard.
 *
 * We leverage the ScriptEngineManager to dynamically obtain and execute scripts based on their MIME types.
 *
 * The class is designed to be instantiated with a well-known IRI an RDF4J model containing the knowledge graph,
 * and an optional secrets provider for accessing sensitive data.
 *
 * @see systems.symbol.intent.AbstractIntent
 * @see systems.symbol.RDF
 * @see systems.symbol.platform.IQ_NS
 */
public class JSR233 extends AbstractIntent {

private final ScriptEngineManager engineManager = new ScriptEngineManager();
private final I_Secrets secrets;

/**
 * Constructs a new JSR233 intent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public JSR233(IRI self, Model model) {
boot(self, model);
this.secrets = null;
}

/**
 * Constructs a new JSR233 intent with the provided RDF4J model and self identity.
 *
 * @param model The RDF4J model associated with the intent.
 * @param self  The self identity of the intent.
 */
public JSR233(IRI self, Model model, I_Secrets secrets) {
boot(self, model);
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
@RDF(IQ_NS.IQ + "script")
public Set<IRI> execute(IRI actor, Resource state, Bindings my) throws StateException {
Set<IRI> done = new HashSet<>();
Literal script = IQScripts.findScript(model, state, null, null);
if (script == null) return done;
try {
Object result = executeScript(script, actor, state, my );
done.add(actor);
log.info("script.result: {} -> {} x {}", state, result, done.size());
} catch (ScriptException e) {
//log.error("script.failed: {}/{} @ {}", e.getLineNumber(), e.getColumnNumber(), e.getFileName());
throw new StateException(e.getMessage(), state, e);
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

//MyFacade.dump(engine.getBindings(ScriptContext.ENGINE_SCOPE), System.out);
try {
engine.setBindings( MyFacade.bind(actor, state, getModel(), my, secrets), ScriptContext.ENGINE_SCOPE);
log.debug("script.eval: {} @ {}", script.stringValue(), state.stringValue() );
return engine.eval(script.stringValue());
} catch (ScriptException e) {
//log.error("script.error: {} @ {} x {}", e.getFileName(),e.getColumnNumber(), e.getLineNumber(), e.getCause() );
throw new ScriptException(e.getMessage()+" -> "+ e.getFileName() +" @" +e.getColumnNumber() +":"+ e.getLineNumber());
} catch (SecretsException e) {
throw new ScriptException(e.getMessage()+" -> "+actor);
}
}
}
