package systems.symbol.cli;

import systems.symbol.io.ImportExport;

import systems.symbol.platform.Workspace;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static systems.symbol.cli.CLIContext.CODENAME;

@CommandLine.Command(name = "init", description = "Bootstrap your new "+CODENAME)
public class InitCommand extends AbstractCLICommand {
    @CommandLine.Option(names = "--from", required=false, description = "Import from file/folder")
    File from;

    @CommandLine.Option(names = "--home", required=false, description = "Home file/folder", defaultValue = "./"+CODENAME)
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
        log.info("iq.cli.home: " + home.getAbsolutePath());
        home.mkdirs();

        Workspace workspace = new Workspace(home);
        log.info("iq.cli.init: {} -> {}", context.getIdentity(), workspace.getIdentity());
        if (from==null) return;

        // import
        if (from.exists()) {
            display("importing from: "+this.from.getAbsolutePath());
            ImportExport.load(context, this.from, this.forceDelete);
        } else {
            display("missing file/folder: "+this.from.getAbsolutePath());
        }
    }
}
