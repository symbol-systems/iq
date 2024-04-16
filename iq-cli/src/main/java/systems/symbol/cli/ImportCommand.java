package systems.symbol.cli;

import com.github.freva.asciitable.AsciiTable;
import picocli.CommandLine;
import systems.symbol.io.ImportExport;
import systems.symbol.rdf4j.io.BootstrapLoader;

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

        doImport();
        return 0;
    }

    private void doImport() throws IOException {
        System.out.printf("importing from: %s\n", from.getAbsolutePath());
        BootstrapLoader load = ImportExport.load(context, from, forceDelete);

        String[] columns = { "total", "assets", "rdf", "errors" };
        Object[] row = { load.total_files, load.total_asset_files, load.total_rdf_files, load.total_errors };
        System.out.println(AsciiTable.getTable(columns, new Object[][] { row }));
    }
}
