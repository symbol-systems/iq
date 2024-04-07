package systems.symbol.rdf4j;

import systems.symbol.io.Fingerprint;
import systems.symbol.rdf4j.util.FakeReturn;
import systems.symbol.rdf4j.util.SupportedScripts;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class TripleScript {
    private static final Logger log = LoggerFactory.getLogger(TripleScript.class);
//    static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    public TripleScript() {
    }

    public String evaluate(TripleSource tripleSource, String script, String scriptEngineName, Object ...args) throws ValueExprEvaluationException {
        try {
            TripleScriptContext context = new TripleScriptContext();
            List _args = new ArrayList();
            context.getBindings().put("args", _args );
            context.getBindings().put("triples", tripleSource );

            for(int i=0; i<args.length;i++) {
                if (args[i] instanceof Literal) {
                    _args.add( ((Literal) args[i]).stringValue() );
                } else {
                    _args.add( args[i].toString() );
                }
            }
            log.info("script.evaluate: {} -> {}", script, context.getBindings().keySet());
            Object value = execute(script, scriptEngineName, context);
            log.info("script.evaluated: {}", value);
            return value.toString();
        } catch (ScriptException e) {
            log.error("script.failed: "+scriptEngineName, e);
            return null;
        }
    }
    /**
     * Instantiates a JSR232 script.
     *
     * @return The result of the script
     */

    public Object execute(String script, String scriptEngineType, TripleScriptContext context) throws ScriptException {
        ScriptEngine scriptEngine = SupportedScripts.getScriptEngine(scriptEngineType);
        if (scriptEngine == null) throw new ScriptException("Unsupported script: " + scriptEngineType);
        try {
//            System.out.println(">>"+script);
            log.trace("script.fingerprint: {}", Fingerprint.toMD5(script));
            Object r = scriptEngine.eval(script, context);
            log.trace("script.result: {}", r);
            return r;
        } catch (ScriptException e) {
            throw new ScriptException(e.getMessage(), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
        } catch (NoSuchAlgorithmException e) {
            throw new ScriptException(e.getMessage());
        }

    }

}
