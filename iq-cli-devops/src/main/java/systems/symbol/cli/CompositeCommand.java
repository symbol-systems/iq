package systems.symbol.cli;

import java.io.IOException;

public abstract class CompositeCommand extends AbstractCLICommand {

public CompositeCommand(CLIContext context) throws IOException {
super(context);
if (!context.isInitialized()) {
return;
}
//subCommands(cli);
}

//protected CommandLine subCommands(CommandLine commands) throws IQException {
//IRI commandsIRI = context.iq.toIRI("iq/cli-models.sparql");
//IQ IQ = context.getIQBase();
//SPARQLMapper sparql = new SPARQLMapper(iqBase);
//List<Map<String,Object>> models = sparql.models(commandsIRI);
//if (models!=null) return subCommands(commands, models);
//else return commands;
//}

//protected CommandLine subCommands(CommandLine commands, List<Map<String,Object>> models)  {
//models.forEach(model -> {
//Object name = model.get("name");
//log.info("iq.models.cmd: " + name);
//if (name!=null && !name.isEmpty()) {
//try {
//commands.addSubcommand(name.toString(), (Callable) () -> {
//List<Map<String,Object>> models1 = Display.models(context.iq, name.toString());
//if (models1 !=null) Display.table(System.out, models1);
//return models1;
//});
//} catch (Exception e) {
//log.error("iq.cli.list.subcommand.failed:"+name.toString(), e);
//}
//}
//});
//return commands;
//}
}
