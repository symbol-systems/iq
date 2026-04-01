package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.io.Display;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "models", description = "List of models from this " + I_Self.CODENAME)
public class ModelsCommand extends CompositeCommand {
    private static final Logger log = LoggerFactory.getLogger(ModelsCommand.class);
    
    @CommandLine.Option(names = {"--format"}, description = "Output format: list, json, table", defaultValue = "list")
    String format = "list";

    public ModelsCommand(CLIContext context, CommandLine cli) throws IOException {
        super(context);
        if (!context.isInitialized()) {
            return;
        }
        // subCommands(cli);
    }

    @Override
    public Object call() throws Exception {
        if (!context.isInitialized()) {
            log.error("iq.models.error: IQ not initialized");
            return null;
        }
        
        IQStore iq = context.newIQBase();
        try {
            List<Map<String, Object>> models = Display.models(iq, "index");
            
            if (models == null || models.isEmpty()) {
                log.warn("iq.models: no models found");
                log.info("iq.models.missing: no models in realm");
                return null;
            }
            
            log.info("iq.models: {} model(s) available", models.size());
            
            if ("json".equalsIgnoreCase(format)) {
                for (Map<String, Object> model : models) {
                    log.info("{}", model);
                }
            } else if ("table".equalsIgnoreCase(format)) {
                log.info(String.format("%-30s | %-50s | %-20s", "Model", "ID", "Type"));
                log.info("-".repeat(110));
                for (Map<String, Object> model : models) {
                    Object label = model.get("label");
                    Object id = model.get(NS.KEY_AT_ID);
                    Object type = model.get("type");
                    log.info(String.format("%-30s | %-50s | %-20s", 
                            label != null ? label : "(unnamed)", 
                            id != null ? id : "(unknown)",
                            type != null ? type : "(unknown)"));
                }
            } else {
                // default list format
                for (Map<String, Object> model : models) {
                    Object label = model.get("label");
                    Object id = model.get(NS.KEY_AT_ID);
                    
                    if (label != null) {
                        log.info("  ✓ {} @ {}", label, id);
                    } else {
                        log.info("  ✓ {}", model);
                    }
                    
                    // Show additional metadata if available
                    Object version = model.get("version");
                    if (version != null) {
                        log.info("    version: {}", version);
                    }
                    Object provider = model.get("provider");
                    if (provider != null) {
                        log.info("    provider: {}", provider);
                    }
                }
            }
            
            log.info("iq.models.found: {}", models.size());
            return models;
        } catch (Exception e) {
            log.error("iq.models.error: {}", e.getMessage(), e);
            return null;
        } finally {
            try {
                iq.close();
            } catch (Exception ignored) {}
        }
    }

}
