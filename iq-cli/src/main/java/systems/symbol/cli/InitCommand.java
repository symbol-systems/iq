package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.io.ImportExport;
import systems.symbol.platform.I_Self;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "init", description = "Bootstrap your new "+I_Self.CODENAME)
public class InitCommand extends AbstractCLICommand {
    private static final Logger log = LoggerFactory.getLogger(InitCommand.class);
    @CommandLine.Option(names = "--from", required=false, description = "Import from file/folder")
    File from;

    @CommandLine.Option(names = "--home", required=false, description = "Home file/folder", defaultValue = "./"+ I_Self.CODENAME)
    File home;

	@CommandLine.Option(names = "--store", description = "The store name", defaultValue = "default")
    String storename;

    @CommandLine.Option(names = "--force", description = "delete before import", defaultValue = "false")
    boolean forceDelete;

    public InitCommand(CLIContext context) throws IOException {
        super(context);
    }

    @Override
    public Object call() throws Exception {
        doInitialize();
        return 0;
    }

    private void doInitialize() throws IOException, URISyntaxException {
        if (!home.mkdirs()) {
            log.warn("Home folder could not be created; {}", home.getAbsolutePath());
            return;
        }
        if (from==null) {
            log.warn("missing --from <import-folder>");
            return;
        }
        if (!from.exists()) {
            display("empty file/folder: "+this.from.getAbsolutePath());
            return;
        }

        log.info("Initialize {} from: {}", CODENAME, this.from.getAbsolutePath());
        ImportExport.load(context, this.from, this.forceDelete);
        AboutCommand.displaySelf(context);
    }
}
