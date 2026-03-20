package systems.symbol.realm;

import org.apache.commons.vfs2.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.I_Corpus;
import systems.symbol.finder.SearchMatrix;
import systems.symbol.platform.I_StartStop;
import systems.symbol.platform.RDFConfigFactory;
import systems.symbol.rdf4j.Facts;
import systems.symbol.secrets.*;
import systems.symbol.string.PrettyString;
import systems.symbol.trust.I_KeyStore;
import systems.symbol.trust.VFSKeyStore;
import systems.symbol.vfs.MyVFS;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static systems.symbol.COMMONS.IQ;

public class RealmManager implements RepositoryResolver, I_StartStop, I_Realms {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final RepositoryManager manager;
    protected FileSystemManager vfs;
    protected FileObject home, lake;
    protected DynamicModelFactory dmf = new DynamicModelFactory();
    protected String defaultType = "default";
    protected I_SecretsStore secrets;
    protected Map<IRI, Realm> realms = new HashMap<>();
    protected Map<IRI, SearchMatrix> searchers = new HashMap<>();

    public RealmManager() throws Exception {
        this(new File("." + IQ.toLowerCase()));
    }

    public RealmManager(File home) throws Exception {
        new File(home, "repositories").mkdirs();
        this.vfs = new MyVFS();
        this.home = vfs.resolveFile(home, ".");
        this.lake = vfs.resolveFile(home, "lake");
        log.info("realms.home: {}", this.home.getPublicURIString());
        log.info("realms.lake: {}", this.lake.getPublicURIString());
        this.manager = new SafeRepositoryManager(home);
        this.secrets = SecretsStoreFactory.createDefault(home);
        log.info("realms.boot: {}", new Date());
    }

    private I_Realm newRealm(IRI self, Model model) throws SecretsException {
        if (realms.containsKey(self)) {
            return realms.get(self);
        }
        log.debug("realm.repo.get: {}", self.stringValue());
        Repository repo = getRepository(self.stringValue());
        if (repo == null) {
            log.warn("realm.repo.missing: {}", self);
            return null;
        }
        Realm realm = null;
        try {
            I_Secrets secrets = this.secrets.getSecrets(self);
            I_KeyStore keys = getKeyStore(self);
            log.debug("realm.secrets: {} x {} == {}", self, model.size(), secrets != null);
            realm = new Realm(self, model, repo, secrets, this.vfs, keys.keys());
            realms.put(self, realm);
            searchers.put(self, new SearchMatrix());
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

    public I_Realm newRealm(IRI self) throws SecretsException, PlatformException {
        Realm realm = realms.get(self);
        log.debug("realm.found: {} -> {}", realms.keySet(), realm != null);
        if (realm != null)
            return realm;
        Repository repo = getRepository(self.stringValue());
        if (repo == null)
            throw new PlatformException("Cannot find realm: " + self);

        Model model = Facts.clone(repo);
        log.debug("realm.model: {} -> {}", self, model.size());
        return newRealm(self, model);
    }

    @Override
    public Repository getRepository(String self) throws RepositoryException, RepositoryConfigException {
        return addRepository(self, PrettyString.sanitize(self), defaultType);
    }

    protected Repository addRepository(String self, String id, String type)
            throws RepositoryException, RepositoryConfigException {
        Repository repo = this.manager.getRepository(id);
        log.debug("realm.get: {} -> {} -> {}", self, id, repo == null ? false : repo.isInitialized());
        if (repo != null) {
            return repo;
        }
        try {
            RepositoryConfig config = RDFConfigFactory.toConfig(Values.iri(self), id, type);
            log.debug("realm.add: {} -> {} -> {} --> {}", id, self, type, config.getID());
            this.manager.addRepositoryConfig(config);
            repo = this.manager.getRepository(id);
            repo.init();
        } catch (IOException e) {
            throw new RepositoryConfigException(e.getMessage());
        }
        return repo;
    }

    public FileObject getHome() {
        return home;
    }

    public FileObject getLake() {
        return lake;
    }

    @Override
    public Set<IRI> getRealms() {
        return realms.keySet();
    }

    public I_Realm getRealm(String self) throws SecretsException {
        return getRealm(Values.iri(self.contains(":") ? self : self + ":"));
    }

    public I_Realm getRealm(IRI self) throws SecretsException {
        return realms.get(self);
    }

    protected I_KeyStore getKeyStore(IRI self) throws Exception {
        FileObject keysHome = getHome().resolveFile("keys/" + toID(self));
        return new VFSKeyStore(vfs, keysHome);
    }

    protected String toID(IRI self) {
        return PrettyString.sanitize(self.stringValue());
    }

    @Override
    public void start() throws Exception {
        log.info("realm.start");
    }

    @Override
    public void stop() {
        for (String id : this.manager.getRepositoryIDs()) {
            log.debug("realm.stop: {}", id);
            this.manager.getRepository(id).shutDown();
        }
        this.manager.shutDown();
        log.info("realm.shutdown: {}", this.manager.getRepositoryIDs());
    }

    @Override
    public I_Corpus<IRI> searcher(IRI realm) {
        return searchers.get(realm);
    }

    public void index(I_Realm realm) {
        SearchMatrix searcher = searchers.get(realm.getSelf());
        if (searcher == null) {
            log.warn("realm.index.missing: {}", realm.getSelf());
            return;
        }
        try (RepositoryConnection connection = realm.getRepository().getConnection()) {
            try (RepositoryResult<Statement> contents = connection.getStatements(null,
                    RDF.VALUE, null)) {
                searcher.reindex(contents.iterator(), realm.getSelf());
            }
        }
    }
}
