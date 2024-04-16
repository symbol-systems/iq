package systems.symbol.cli;

import systems.symbol.io.Display;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.store.IQ;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "models", description = "List of models from this "+CODENAME)
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
