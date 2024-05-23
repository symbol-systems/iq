package systems.symbol.cli;

import picocli.CommandLine;
import systems.symbol.io.Display;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.store.IQ;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "models", description = "List of models from this "+ I_Self.CODENAME)
public class ModelsCommand extends CompositeCommand {

public ModelsCommand(CLIContext context, CommandLine cli) throws IOException {
super(context);
if (!context.isInitialized()) {
return;
}
//subCommands(cli);
}

@Override
public Object call() throws Exception {
IQ iq = context.newIQBase();
List<Map<String,Object>> models = Display.models(iq, "index");
iq.close();
if (models==null) {
log.info("iq.models.missing: ");
return null;
}
log.info("iq.models.found: " + models.size());
models.forEach(model -> {
Object label = model.get("label");
if (label != null) {
log.info("> " + label + " @ " + model.get(NS.KEY_AT_ID));
} else {
log.info("> " + model);
}
});
return models;
}

}
