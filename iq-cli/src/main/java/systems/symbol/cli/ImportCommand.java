package systems.symbol.cli;

import systems.symbol.io.ImportExport;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "import", description = "Import new knowledge into "+CODENAME)
public class ImportCommand extends AbstractCLICommand{
@CommandLine.Option(names = "--from", description = "Load assets from this folder")
File from;
@CommandLine.Option(names = "--force", description = "Remove existing before importing", defaultValue = "false")
boolean forceDelete = false;

public ImportCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {

if (from==null||!from.exists()) {
display("missing --from");
return 1;
}
log.info("iq.cli.learn.deploy: "+from.getAbsolutePath());
ImportExport.load(context, from, forceDelete);
log.info("iq.cli.learn.done");
return 0;
}
}
