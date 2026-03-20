package systems.symbol.cli;

import picocli.CommandLine;
import systems.symbol.io.Display;
import systems.symbol.io.StreamCopy;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;

import java.io.File;
import java.io.IOException;

@CommandLine.Command(name = "script", description = "Run an IQ script")
public class ScriptCommand extends AbstractCLICommand {
    @CommandLine.Option(names = "--list", description = "List available scripts")
    boolean listScripts = false;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Script path or asset name")
    String script = "";

    public ScriptCommand(CLIContext context) throws IOException {
        super(context);
    }

    public Object call() throws Exception {
        File assets = context.getAssetsHome();
        File scriptsHome = new File(assets, "scripts");

        if (listScripts) {
            listScripts(assets);
            return 0;
        }

        if (script == null || script.isBlank()) {
            display("No script provided. Pass path or name, or --list");
            return 1;
        }

        File scriptFile = new File(script);
        if (!scriptFile.exists()) {
            scriptFile = new File(scriptsHome, script);
        }
        if (!scriptFile.exists() && !script.endsWith(".groovy") && !script.endsWith(".sparql")) {
            // try asset extension
            File candidate = new File(scriptsHome, script + ".sparql");
            if (candidate.exists()) scriptFile = candidate;
        }

        if (!scriptFile.exists()) {
            display("Script not found: " + script);
            return 1;
        }

        if (scriptFile.getName().endsWith(".sparql")) {
            runSparqlScript(scriptFile);
            return 0;
        }

        if (scriptFile.getName().endsWith(".groovy")) {
            display("Groovy scripts are not supported in iq-cli. Use iq-cli-pro for Groovy execution.");
            return 1;
        }

        display("Unsupported script type: " + scriptFile.getName());
        return 1;
    }

    private void listScripts(File assetsHome) {
        File scripts = new File(assetsHome, "scripts");
        if (!scripts.exists() || !scripts.isDirectory()) {
            display("No scripts folder found: " + scripts.getAbsolutePath());
            return;
        }
        display("scripts:");
        for (File candidate : scripts.listFiles()) {
            if (candidate.isFile()) {
                display("  " + candidate.getName());
            }
        }
    }

    private void runSparqlScript(File scriptFile) throws IOException {
        String query = StreamCopy.toString(scriptFile);
        IQStore iq = context.newIQBase();
        SPARQLMapper mapper = new SPARQLMapper(iq);
        if (mapper.isSelect(query) || query.toUpperCase().contains("ASK")) {
            Display.display(mapper.query(query, null));
        } else {
            var graphResult = mapper.graph(query, null);
            if (graphResult != null) {
                while (graphResult.hasNext()) {
                    display(graphResult.next().toString());
                }
            } else {
                display("Script query is not SELECT/ASK/CONSTRUCT/DESCRIBE.");
            }
        }
        iq.close();
    }

}
