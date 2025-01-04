package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.RDF;
import systems.symbol.agent.Facades;
import systems.symbol.tools.TrustedAPIs;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.util.SupportedScripts;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyStrings;

import javax.script.*;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * An intent that executes scripts using JSR-233 - the Java Scripting API.
 *
 * This class represents an intent capable of executing scripts written in
 * various scripting languages supported
 * by the JSR-233 (Java Scripting API) standard.
 *
 * We leverage the ScriptEngineManager to dynamically obtain and execute scripts
 * based on their MIME types.
 *
 * The class is designed to be instantiated with a well-known IRI an RDF4J model
 * containing the knowledge graph,
 * and an optional secrets provider for accessing sensitive data.
 *
 * @see systems.symbol.intent.AbstractIntent
 * @see systems.symbol.RDF
 * @see systems.symbol.platform.IQ_NS
 */
public class JSR233 extends AbstractIntent {
    private final ScriptEngineManager engineManager = new ScriptEngineManager();
    private final I_Secrets secrets;
    private final I_Contents scripts;

    /**
     * Constructs a new JSR233 intent with the provided RDF4J model and self
     * identity.
     *
     * @param model The RDF4J model associated with the intent.
     * @param self  The self identity of the intent.
     */
    public JSR233(IRI self, Model model) {
        boot(self, model);
        this.secrets = null;
        this.scripts = new ModelScriptCatalog(model);
    }

    /**
     * Constructs a new JSR233 intent with the provided RDF4J model and self
     * identity.
     *
     * @param model The RDF4J model associated with the intent.
     * @param self  The self identity of the intent.
     */
    public JSR233(IRI self, Model model, Model thoughts, I_Secrets secrets, I_Contents scripts)
            throws SecretsException {
        boot(self, thoughts);
        this.scripts = scripts;
        this.secrets = TrustedAPIs.trusted(model, self, secrets);
    }

    /**
     * Executes the JSR-233 script based on the provided actor and resource.
     *
     * @param actor The actor of the execution.
     * @param state The resource containing the script.
     * @return A set of IRIs indicating the completion of execution.
     */
    @Override
    @RDF(IQ_NS.IQ + "script")
    public Set<IRI> execute(IRI actor, Resource state, Bindings my) throws StateException {
        Set<IRI> done = new HashSet<>();
        Literal script = scripts.getContent(state, null);
        if (script == null) {
            log.error("script.missing: {}", state);
            return done;
        }
        try {
            Object result = executeScript(script, actor, state, my);
            done.add(actor);
            log.info("script.done: {} -> {} --> {}", state, result, PrettyStrings.pretty(my));
        } catch (ScriptException e) {
            Throwable cause = e.getCause().getCause();
            String error = cause == null ? e.getMessage() : cause.getMessage();
            log.warn("script.failed: {} ({}:{}) == {}", state, e.getLineNumber(), e.getColumnNumber(),
                    error);
            throw new StateException(error, state);
        } catch (SecretsException e) {
            throw new StateException(e.getMessage(), state, e);
        }
        return done;
    }

    /**
     * Executes the script using the provided script engine and bindings.
     *
     * @param script The script to execute.
     * @param state  The state that trigger the intent
     * @param my     The bindings for script runtime.
     * @return The result of the script execution.
     * @throws ScriptException If an error occurs during script execution.
     */
    Object executeScript(Literal script, IRI actor, Resource state, Bindings my)
            throws ScriptException, SecretsException {
        String mime = SupportedScripts.toMimeType(script.getDatatype());
        if (mime == null)
            return null;
        ScriptEngine engine = engineManager.getEngineByMimeType(mime);
        if (engine == null) {
            return null;
        }
        ScriptContext sc = new SimpleScriptContext();
        StringWriter out = new StringWriter();
        sc.setWriter(out);
        sc.setBindings(Facades.trust(actor, state, getModel(), my, secrets), ScriptContext.ENGINE_SCOPE);
        sc.setAttribute("log", log, ScriptContext.ENGINE_SCOPE);

        Object eval = engine.eval(script.stringValue(), sc);
        log.debug("script.eval: {} @ {} ==> {}", script.stringValue(), state.stringValue(), eval);
        log.info("script.logged: %o", out.toString());
        return eval;
    }
}
