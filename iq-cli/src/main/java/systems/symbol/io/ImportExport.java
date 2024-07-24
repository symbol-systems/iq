package systems.symbol.io;

import systems.symbol.cli.CLIContext;
import systems.symbol.rdf4j.io.BootstrapLake;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImportExport {
protected static final Logger log = LoggerFactory.getLogger(ImportExport.class);


public static BootstrapLake getBulkAssetLoader(CLIContext context, boolean forceDelete) {
RepositoryConnection connection = context.getRepository().getConnection();
return new BootstrapLake(context.getSelf().stringValue(), connection, forceDelete, true, true, true);
}

public static void restore(CLIContext context) throws IOException {
BootstrapLake loader = getBulkAssetLoader(context, false);
log.info("iq.loader.reload.backups: "+ context.backups.getAbsolutePath());
loader.deploy(context.backups);
loader.close();
}

public static BootstrapLake load(CLIContext context, File userFile, boolean forceDelete) throws IOException {
BootstrapLake loader = getBulkAssetLoader(context, forceDelete);
if (userFile!=null && userFile.exists()) {
log.info("iq.loader.load.file: "+userFile.getAbsolutePath());
loader.deploy(userFile);
}
return loader;
}

public static void export(CLIContext context, File toFolder, String comment) throws IOException {
toFolder.mkdirs();
Repository repository = context.getRepository();
File outputFile = new File("export.tll");
RDFFormat format = RDFFormat.TURTLE;
try (RepositoryConnection connection = repository.getConnection()) {
RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true, context.getSelf());

try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
Rio.write(statements, outputStream, format);
}
}

log.info("iq.exported: {} -> {}", context.getSelf(), outputFile.getAbsolutePath());
}
}
