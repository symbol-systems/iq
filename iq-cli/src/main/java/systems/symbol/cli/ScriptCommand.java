package systems.symbol.cli;

import picocli.CommandLine;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;

@CommandLine.Command(name = "script", description = "Run an IQ script")
public class ScriptCommand extends AbstractCLICommand {
    @CommandLine.Parameters(index = "0", description = "The IRI or path of the script to run.")
    String script = "scripts/index.groovy";

    public ScriptCommand(CLIContext context) throws IOException {
        super(context);
    }

    public Object call() throws Exception {

        File scriptFile = new File(this.script);
        if (scriptFile.exists()) {
            runLocal(scriptFile);
            return 0;
        }

//        IQ iq = context.newIQBase();

//        IRI scriptIRI = iq.toIRI(this.script);
//        IRI jobIRI = iq.toIRI(UUID.randomUUID().toString());
//        log.info("iq.script.job: {} @ {}" , jobIRI, script);
//        Script script = new Script(iq, jobIRI);
//        IRI done = script.execute(scriptIRI);
//        if (done!=null) {
//            log.info("iq.script.done: {} @ {}" , done, jobIRI);
//        } else {
//            log.info("iq.script.null: {} @ {}", scriptIRI, jobIRI);
//        }
        return 0;
    }

    private void runLocal(File script) throws IOException, ScriptException {
        log.info("iq.script.run: {}" , script.getAbsoluteFile());
//        TripleScript scripts = new TripleScript();
//        TripleScriptContext scriptContext = new TripleScriptContext();
////        scriptContext.setBindings(bindings);
//        String scriptBody = StreamCopy.load(script);
//        Object result = scripts.execute(scriptBody, "groovy", scriptContext);
//        if (result!=null) System.out.println(result);
    }

}
