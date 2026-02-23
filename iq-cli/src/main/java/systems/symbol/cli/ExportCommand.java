package systems.symbol.cli;

import picocli.CommandLine;
import systems.symbol.io.FileHelper;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

import static systems.symbol.io.ImportExport.export;

@CommandLine.Command(name = "export", description = "Export knowledge from this " + IQ_NS.IQ)
public class ExportCommand extends AbstractCLICommand {
@CommandLine.Option(names = "--to", description = "Export to this folder", defaultValue = IQ_NS.IQ + ".ttl")
File to;
@CommandLine.Option(names = "--comment", description = "Comment for the exported file")
String comment = "knowledge export";
@CommandLine.Option(names = "--ns", description = "The namespace for your " + IQ_NS.IQ, defaultValue = "urn:"+ IQ_NS.IQ + ":")
String ns;

public ExportCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (to==null||!to.exists()) {
display("missing --to");
return 1;
}
File toFolder = FileHelper.toTodayFile(to);
System.out.printf("export <%s> to %s", context.getSelf(), to.getAbsolutePath());
export(context, toFolder, comment);
return 0;
}
}
