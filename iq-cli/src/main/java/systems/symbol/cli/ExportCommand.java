package systems.symbol.cli;

import systems.symbol.io.FileHelper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static systems.symbol.io.ImportExport.export;
import static systems.symbol.COMMONS.CODENAME;

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
