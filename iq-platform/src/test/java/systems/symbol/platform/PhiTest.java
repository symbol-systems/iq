package systems.symbol.platform;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;

import systems.symbol.lake.BootstrapLake;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.rdf4j.store.MemoryRDFSRepository;
import systems.symbol.realm.About;
import systems.symbol.util.Stopwatch;

public class PhiTest {
File assetsRoot = new File("src/test/resources/assets/");

@Test
void testPhi() throws RepositoryException, IOException {
Stopwatch stopWatch = new Stopwatch();
MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
SailRepositoryConnection connection = memoryRDFSRepository.getConnection();
BootstrapLake loader = new BootstrapLake(IQ_NS.TEST, connection, false, true, false, false);

System.out.println("phi.loader: " + assetsRoot.getAbsolutePath());
loader.deploy(assetsRoot);
System.out.println("phi.loaded: " + stopWatch.elapsed());

LiveModel liveModel = new LiveModel(connection);

System.out.println("phi.modelled: " + stopWatch.elapsed());
double phi = About.computePhi(liveModel);
System.out.println("phi.score: " + phi);
System.out.println("phi.computed: " + stopWatch.elapsed());
double phi_norm = About.computePhiNormal(liveModel);
System.out.println("phi.normalized: " + stopWatch.elapsed());
System.out.println("phi.norm.score: " + phi_norm);
memoryRDFSRepository.shutDown();
assert phi_norm > 0.0;
}
}
