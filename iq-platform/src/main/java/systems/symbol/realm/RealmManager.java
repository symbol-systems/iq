package systems.symbol.realm;

import org.apache.commons.vfs2.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.FactFinder;
import systems.symbol.platform.I_StartStop;
import systems.symbol.platform.RDFConfigFactory;
import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.secrets.*;
import systems.symbol.string.PrettyString;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpaceManager implements RepositoryResolver, I_StartStop {
private final Logger log = LoggerFactory.getLogger(getClass());
private final RepositoryManager manager;
FileSystemManager vfs;
File home, repoHome, logHome, importsHome, vaultHome, indexHome;
I_SecretsStore secrets;
DynamicModelFactory dmf = new DynamicModelFactory();
String bootType = "memory", defaultType = "default";

public SpaceManager(File home) throws IOException {
this.home = home;
this.logHome = new File(home, "log");
this.importsHome = new File(home, "import");
this.vaultHome = new File(home, "vault");
this.repoHome = new File(home, "repo");
this.indexHome = new File(home, "index");
this.logHome.mkdirs();
this.importsHome.mkdirs();
this.vaultHome.mkdirs();
this.indexHome.mkdirs();
this.repoHome.mkdirs();
this.vfs = VFS.getManager();
this.manager = new LocalRepositoryManager(repoHome);
this.secrets = new BasicFileVault(vaultHome);
}

public SpaceManager(IRI self, File home) throws Exception {
this(home);
boot(self, this.importsHome);
}

public void boot(IRI self, File importsHome) throws Exception {
File[] files = importsHome.listFiles();
if (files != null) {
for (File file : files) {
if (file.isDirectory() && !file.getName().startsWith(".")) {
String space = self.stringValue() + "_" + file.getName();
log.info("space.boot: {} -> {} @ {}", space, bootType, file.getAbsolutePath());
Repository repository = addRepository(self, PrettyString.sanitize(space), bootType);
try (RepositoryConnection connection = repository.getConnection()) {
BootstrapLoader loader = new BootstrapLoader(space, connection, true, true, true, true);
loader.deploy(file);
}
}
}
}
start();
}

public I_Realm getSpace(IRI self, Model model) throws SecretsException {
Map<IRI, FactFinder> finders = new HashMap<>();
Repository repo = getRepository(self.stringValue());

String id = PrettyString.sanitize(self.stringValue());
File finderFile = new File(indexHome, id);
FactFinder finder = new FactFinder(repo, finderFile, null, 10, 0.7);

File[] finderFiles = indexHome.listFiles();
if (finderFiles!=null && finderFiles.length==0) {
addFactFinder(self, id, repo, finders);
} else if (finderFiles!=null) {
for(File index : finderFiles) {
addFactFinder(self, index.getName(), repo, finders);
}
}
I_Secrets secrets = this.secrets.getSecrets(self);

log.info("space.new: {} = {}", self, model.size());
return new Realm(self, model, this.manager.getRepository(id), finder, secrets, this.vfs);
}

public FactFinder addFactFinder(IRI self, String id, Repository repo, Map<IRI, FactFinder> finders) {
FactFinder finder = new FactFinder(repo, finderFile, null, 10, 0.7);
finders.put(self, finder);
return finder;
}

public I_Realm getSpace(IRI self) throws SecretsException, IOException {
Repository repo = getRepository(self.stringValue());

// hydrate  model as a repo cache
DynamicModel model = dmf.createEmptyModel();
try (RepositoryConnection conn = repo.getConnection()) {
RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
for (Statement s : statements) {
model.add(s);
}
}
log.info("space.model: {} -> {}", self, model.size());
return getSpace(self, model);
}

@Override
public Repository getRepository(String self) throws RepositoryException, RepositoryConfigException {
return addRepository(Values.iri(self), PrettyString.sanitize(self), defaultType);
}

public Repository addRepository(IRI self, String id, String type) throws RepositoryException, RepositoryConfigException {
Repository repo = this.manager.getRepository(id);
if (repo!=null) return repo;
try {
RepositoryConfig config = RDFConfigFactory.toConfig(self, id, type);
log.info("space.add: {} -> {} -> {} --> {}", self, type, config.getID(), config.getTitle());
this.manager.addRepositoryConfig(config);
repo = this.manager.getRepository(id);
repo.init();
} catch (IOException e) {
throw new RepositoryConfigException(e.getMessage());
}
return repo;
}

public void fedex() {
//FedXFactory.newFederation();

}

@Override
public void start() throws Exception {
}

@Override
public void stop() {
for(String id: this.manager.getRepositoryIDs()) {
this.manager.getRepository(id).shutDown();
}
this.manager.shutDown();
}
}
