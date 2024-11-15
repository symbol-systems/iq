package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.MemoryRDFSRepository;
import java.io.File;
import java.io.IOException;

public class BootstrapLakeTest {
File assetsRoot = new File("src/test/resources/assets/");

@Test
public void testDeployResourcesFolder() throws IOException {
assert assetsRoot.exists();
assert assetsRoot.isDirectory();
assert assetsRoot.exists();

MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
SailRepositoryConnection connection = memoryRDFSRepository.getConnection();

BootstrapLake loader = new BootstrapLake(IQ_NS.TEST, connection, true, true, true, false);
// TripleSource tripleSource = memoryRDFSRepository.getTripleSource(true);
// loader.DEBUG = true;

assert loader.getSelf().toString().equals(IQ_NS.TEST);
assert loader.getSince() == 0; // ignore last-modified

System.out.println("bulk.loader.folder: " + assetsRoot.getAbsolutePath());
loader.deploy(assetsRoot);

try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
Model model = Remodels.model(statements);
assert model.size() > 10;
}
memoryRDFSRepository.shutDown();

}

}
