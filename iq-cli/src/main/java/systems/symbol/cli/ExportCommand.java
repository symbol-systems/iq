package systems.symbol.cli;

import systems.symbol.io.FileHelper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static systems.symbol.cli.CLIContext.CODENAME;
import static systems.symbol.io.ImportExport.export;

@CommandLine.Command(name = "export", description = "Export knowledge from this " + CODENAME)
public class ExportCommand extends AbstractCLICommand {
@CommandLine.Option(names = "--to", description = "Export to this folder", defaultValue = CODENAME + ".ttl")
File to;
@CommandLine.Option(names = "--comment", description = "Comment for the exported file")
String comment = "knowledge export";
@CommandLine.Option(names = "--ns", description = "The namespace for your " + CODENAME, defaultValue = "urn:"+ CODENAME + ":")
String ns;

public ExportCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
to.getParentFile().mkdirs();
log.info("iq.cli.export.ns: " + context.getIdentity());
log.info("iq.cli.export.to: " + to.getAbsolutePath());

// save in today's folder
File toFolder = FileHelper.toTodayFile(to);
export(context, toFolder, comment);
return 0;
}
}
