package systems.symbol.cli;

import systems.symbol.io.FileHelper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static systems.symbol.cli.CLIContext.CODENAME;
import static systems.symbol.io.ImportExport.export;

@CommandLine.Command(name = "backup", description = "Backup knowledge from this "+CODENAME)
public class BackupCommand extends AbstractCLICommand{

    public BackupCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        if (context.isInitialized()) {
            log.info("iq.cli.backup.ns: "+context.getSelf());

            // save in today's folder
            File toFolder = FileHelper.toTodayFile(context.backups);

            // the actual filename is a timestamp
            File dumpFile = new File(toFolder, System.currentTimeMillis() + ".ttl");
            export(context, dumpFile, "backup: "+context.getSelf());
            return context;
        } throw new RuntimeException(("iq.cli.backup.failed"));
    }

}
