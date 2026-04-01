package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@CommandLine.Command(name = "run", description = "Execute an IQ script")
public class RunCommand extends CompositeCommand {
    private static final Logger log = LoggerFactory.getLogger(RunCommand.class);
    
    @CommandLine.Parameters(index = "0", description = "The path of the script / query to run.")
    String path = "";
    
    @CommandLine.Option(names = {"--lang"}, description = "Script language: sparql, groovy, js")
    String language = null;
    
    @CommandLine.Option(names = {"--bindings"}, description = "Bindings as key=value pairs")
    String bindings = null;
    
    @CommandLine.Option(names = {"--output"}, description = "Output format: table, json, ttl", defaultValue = "table")
    String outputFormat = "table";

    public RunCommand(CLIContext context, CommandLine commands) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) {
            log.error("iq.run.failed");
            return null;
        }
        
        if (path == null || path.isEmpty()) {
            log.error("iq.run.error: path is required");
            return null;
        }
        
        // Detect language from file extension if not specified
        String lang = language;
        if (lang == null) {
            if (path.endsWith(".sparql") || path.endsWith(".rq")) {
                lang = "sparql";
            } else if (path.endsWith(".groovy")) {
                lang = "groovy";
            } else if (path.endsWith(".js")) {
                lang = "js";
            } else {
                lang = "sparql"; // default
            }
        }
        
        log.info("iq.run: {} [{}]", path, lang);
        
        IQStore iq = context.newIQBase();
        try {
            String code = new String(Files.readAllBytes(Paths.get(path)));
            
            if ("sparql".equalsIgnoreCase(lang)) {
                executeSPARQL(iq, code);
            } else if ("groovy".equalsIgnoreCase(lang)) {
                log.warn("iq.run.error: Groovy scripts are not supported in iq-cli-pro. Use iq-mcp for Groovy execution.");
                return null;
            } else if ("js".equalsIgnoreCase(lang)) {
                log.warn("iq.run.error: JavaScript scripts are not supported in iq-cli-pro yet.");
                return null;
            } else {
                log.warn("iq.run.error: unsupported language: {}", lang);
                return null;
            }
            
            return "run:" + path + ":" + lang;
        } catch (Exception e) {
            log.error("iq.run.error: {} - {}", path, e.getMessage(), e);
            return null;
        } finally {
            iq.close();
        }
    }
    
    private void executeSPARQL(IQStore iq, String sparql) throws Exception {
        RepositoryConnection conn = iq.getConnection();
        
        // Check if it's a SELECT query
        if (sparql.trim().toUpperCase().startsWith("SELECT") || 
            sparql.trim().toUpperCase().startsWith("PREFIX")) {
            
            var tupleQuery = conn.prepareTupleQuery(sparql);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                // Print header
                var bindingNames = result.getBindingNames();
                log.info("Results:");
                StringBuilder header = new StringBuilder();
                for (String name : bindingNames) {
                    header.append(name).append("\t");
                }
                log.info(header.toString());
                
                // Print rows
                int count = 0;
                while (result.hasNext()) {
                    var binding = result.next();
                    StringBuilder row = new StringBuilder();
                    for (String name : bindingNames) {
                        var val = binding.getBinding(name);
                        row.append(val != null ? val.getValue() : "null").append("\t");
                    }
                    log.info(row.toString());
                    count++;
                }
                log.info("{} row(s)", count);
            }
        } else {
            // ASK or UPDATE queries
            boolean result = conn.prepareBooleanQuery(sparql).evaluate();
            log.info("Result: {}", result);
        }
    }
}
