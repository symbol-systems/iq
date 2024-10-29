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
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.FactFinder;
import systems.symbol.platform.I_StartStop;
import systems.symbol.platform.RDFConfigFactory;
import systems.symbol.secrets.*;
import systems.symbol.string.PrettyString;
import systems.symbol.trust.SimpleKeyStore;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static systems.symbol.COMMONS.IQ;

public class RealmManager implements RepositoryResolver, I_StartStop, I_Realms {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected final RepositoryManager manager;
protected final File keysHome;
protected FileSystemManager vfs;
protected File home, logHome, lakeHome, vaultHome, indexHome;
protected DynamicModelFactory dmf = new DynamicModelFactory();
protected String defaultType = "default";
protected I_SecretsStore secrets;
protected Map<IRI, Realm> realms = new HashMap<>();

public RealmManager() throws Exception {
this(new File("." + IQ.toLowerCase()));
}

public RealmManager(File home) throws Exception {
this.home = home;
this.logHome = new File(home, "log");
this.lakeHome = new File(home, "lake");
this.vaultHome = new File(home, "vault");
this.indexHome = new File(home, "index");
this.logHome.mkdirs();
this.lakeHome.mkdirs();
this.vaultHome.mkdirs();
this.indexHome.mkdirs();
this.vfs = VFS.getManager();
this.manager = new SafeRepositoryManager(home);
keysHome = new File(vaultHome, "keys");
keysHome.mkdirs();
File secretsHome = new File(vaultHome, "secrets");
secretsHome.mkdirs();
this.secrets = new PlainPasswordVault(secretsHome);
log.info("realms.boot: {}", new Date());
}

public File getHome() {
return home;
}

public File getVaultHome() {
return vaultHome;
}

public File getLakeHome() {
return lakeHome;
}

public I_Realm getRealm(String self) throws SecretsException {
return getRealm(Values.iri(self.contains(":") ? self : self + ":"));
}

public I_Realm getRealm(IRI self) throws SecretsException {
return realms.get(self);
}

public I_Realm newRealm(IRI self) throws SecretsException, PlatformException {
Realm realm = realms.get(self);
log.debug("realm.found: {} -> {}", realms.keySet(), realm != null);
if (realm != null)
return realm;
Repository repo = getRepository(self.stringValue());
if (repo == null)
throw new PlatformException("Cannot find realm: " + self);
// Facts.clone();
DynamicModel model = dmf.createEmptyModel();
try (RepositoryConnection conn = repo.getConnection()) {
RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
for (Statement s : statements) {
model.add(s);
}
}
log.debug("realm.model: {} -> {}", self, model.size());
return getRealm(self, model);
}

public I_Realm getRealm(IRI self, Model model) throws SecretsException {
if (realms.containsKey(self)) {
return realms.get(self);
}
Repository repo = getRepository(self.stringValue());
if (repo == null) {
log.warn("realm.repo.missing: {}", self);
return null;
}

String id = PrettyString.sanitize(self.stringValue());
File finderFile = new File(indexHome, id);
FactFinder finder = new FactFinder(repo, finderFile, null, 10, 0.7);
Realm realm = null;
try {
File keyHome = new File(keysHome, id);
SimpleKeyStore keys = new SimpleKeyStore(keyHome); // TODO: encrypt
I_Secrets secrets = this.secrets.getSecrets(self);
log.info("realm.secrets: {} x {} == {}", self, model.size(), secrets != null);
realm = new Realm(self, model, this.manager.getRepository(id), finder, secrets, this.vfs, keys.keys());
realms.put(self, realm);
log.debug("realm.cached: {} -> {}", self, realms.keySet());
} catch (SecretsException e) {
log.error("realm.secrets", e);
throw new SecretsException(e.getMessage());
} catch (IOException e) {
log.error("realm.load", e);
} catch (Exception e) {
log.error("realm.error", e);
throw new RuntimeException(e);
}
return realm;
}

@Override
public Set<IRI> getRealms() {
return realms.keySet();
}

@Override
public Repository getRepository(String self) throws RepositoryException, RepositoryConfigException {
return addRepository(self, PrettyString.sanitize(self), defaultType);
}

public Repository addRepository(String self, String id, String type)
throws RepositoryException, RepositoryConfigException {
Repository repo = this.manager.getRepository(id);
if (repo != null)
return repo;
try {
RepositoryConfig config = RDFConfigFactory.toConfig(Values.iri(self), id, type);
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
log.info("realm.start");
}

@Override
public void stop() {
for (String id : this.manager.getRepositoryIDs()) {
log.info("realm.stop: {}", id);
this.manager.getRepository(id).shutDown();
}
log.info("realm.shutdown: {}", home.getAbsolutePath());
this.manager.shutDown();
}
}
