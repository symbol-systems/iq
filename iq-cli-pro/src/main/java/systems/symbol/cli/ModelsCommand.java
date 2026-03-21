package systems.symbol.cli;

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
System.out.println("iq.models.error: IQ not initialized");
return null;
}

IQStore iq = context.newIQBase();
try {
List<Map<String, Object>> models = Display.models(iq, "index");

if (models == null || models.isEmpty()) {
System.out.println("iq.models: no models found");
log.info("iq.models.missing: no models in realm");
return null;
}

System.out.println("iq.models: " + models.size() + " model(s) available");
System.out.println();

if ("json".equalsIgnoreCase(format)) {
for (Map<String, Object> model : models) {
System.out.println(model);
}
} else if ("table".equalsIgnoreCase(format)) {
System.out.println(String.format("%-30s | %-50s | %-20s", "Model", "ID", "Type"));
System.out.println("-".repeat(110));
for (Map<String, Object> model : models) {
Object label = model.get("label");
Object id = model.get(NS.KEY_AT_ID);
Object type = model.get("type");
System.out.println(String.format("%-30s | %-50s | %-20s", 
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
System.out.println("  ✓ " + label + " @ " + id);
} else {
System.out.println("  ✓ " + model);
}

// Show additional metadata if available
Object version = model.get("version");
if (version != null) {
System.out.println("version: " + version);
}
Object provider = model.get("provider");
if (provider != null) {
System.out.println("provider: " + provider);
}
}
}

log.info("iq.models.found: " + models.size());
return models;
} catch (Exception e) {
log.error("iq.models.error: {}", e.getMessage(), e);
System.out.println("iq.models.error: " + e.getMessage());
return null;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

}
