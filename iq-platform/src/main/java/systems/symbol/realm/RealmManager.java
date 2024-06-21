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
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.platform.I_StartStop;
import systems.symbol.platform.RDFConfigFactory;
import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.secrets.*;
import systems.symbol.string.PrettyString;
import systems.symbol.trust.I_KeyStore;
import systems.symbol.trust.SimpleKeyStore;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RealmManager implements RepositoryResolver, I_StartStop, I_Realms {
private final Logger log = LoggerFactory.getLogger(getClass());
private final RepositoryManager manager;
private final I_KeyStore keys;
private final File keysHome;
FileSystemManager vfs;
File home, repoHome, logHome, importsHome, vaultHome, indexHome;
I_SecretsStore secrets;
DynamicModelFactory dmf = new DynamicModelFactory();
String bootType = "memory", defaultType = "default";

public RealmManager(File home) throws Exception {
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
keysHome = new File(vaultHome,"keys");
keysHome.mkdirs();
this.keys = new SimpleKeyStore(keysHome);
File secretsHome = new File(vaultHome,"secrets");
secretsHome.mkdirs();
this.secrets = new PlainPasswordVault(secretsHome);
}

public RealmManager(IRI self, File home) throws Exception {
this(home);
boot(self, this.importsHome);
}

public void boot(IRI self, File importsHome) throws Exception {
File[] files = importsHome.listFiles();
if (files != null) {
for (File file : files) {
if (file.isDirectory() && !file.getName().startsWith(".")) {
String space = self.stringValue() + "_" + file.getName();
log.info("realm.boot: {} -> {} @ {}", space, bootType, file.getAbsolutePath());
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

public I_Realm getRealm(IRI self, Model model) throws SecretsException {
Repository repo = getRepository(self.stringValue());

String id = PrettyString.sanitize(self.stringValue());
File finderFile = new File(indexHome, id);
FactFinder finder = new FactFinder(repo, finderFile, null, 10, 0.7);
log.info("realm.finder: {} = {}", self, finder);

try {
Properties properties = getProperties(id);
File keysFolder = new File(keysHome, id);
SimpleKeyStore keyStore = new SimpleKeyStore(keysFolder);
I_Secrets secrets = this.secrets.getSecrets(self);

log.info("realm.models: {} = {}", self, model.size());
return new Realm(self, model, this.manager.getRepository(id), properties, finder, secrets, this.vfs, keyStore.keys());
} catch (SecretsException e) {
throw new SecretsException(e.getMessage());
} catch (IOException e) {
log.error("realm.load",e);
} catch (Exception e) {
log.error("realm.error",e);
throw new RuntimeException(e);
}
return null;
}

private Properties getProperties(String self) throws IOException {
File propsFile = new File(home, self + ".properties");
Properties properties = new Properties();
if (!propsFile.exists()) {
properties.store(Files.newOutputStream(propsFile.toPath()), "# "+new Date());
} else {
properties.load(new FileInputStream(propsFile));
}

return properties;
}

public I_Realm getRealm(IRI self) throws SecretsException {
Repository repo = getRepository(self.stringValue());

// hydrate  model as a repo cache
DynamicModel model = dmf.createEmptyModel();
try (RepositoryConnection conn = repo.getConnection()) {
RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
for (Statement s : statements) {
model.add(s);
}
}
log.info("realm.model: {} -> {}", self, model.size());
return getRealm(self, model);
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
log.info("realm.add: {} -> {} -> {} --> {}", self, type, config.getID(), config.getTitle());
this.manager.addRepositoryConfig(config);
repo = this.manager.getRepository(id);
repo.init();
} catch (IOException e) {
throw new RepositoryConfigException(e.getMessage());
}
return repo;
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
