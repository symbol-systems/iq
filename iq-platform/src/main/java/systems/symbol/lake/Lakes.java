package systems.symbol.lake;

import java.io.File;
import java.io.IOException;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.realm.I_Realm;
import systems.symbol.realm.PlatformException;
import systems.symbol.realm.RealmManager;

public class Lakes {
    protected static final Logger log = LoggerFactory.getLogger(Lakes.class);

    public static Repository load(File assetsRoot, String context) throws IOException {
        return load(assetsRoot, context, true, true);
    }

    public static Repository load(File assetsRoot, String context, boolean force, boolean deployRDF)
            throws IOException {
        Repository repo = new SailRepository(new MemoryStore());
        log.info("lake.load: {} -> {}", assetsRoot, context);
        log.info("lake.load.options: force:{}, rdf: {}", force, deployRDF);
        try (RepositoryConnection connection = repo.getConnection()) {
            BootstrapLake loader = new BootstrapLake(context, connection, force, deployRDF, true, true);
            loader.deploy(assetsRoot);
            connection.commit();
            connection.close();
            log.info("lake.loaded: {} @ {}", assetsRoot.getAbsolutePath(), loader.getSelf());
        }
        return repo;
    }

    public static void boot(RealmManager realms) throws Exception, PlatformException {
        if (!realms.getLake().exists())
            realms.getLake().createFolder();
        if (realms.getLake().isFile())
            throw new PlatformException("lake.invalid");

        FileObject[] files = realms.getLake().getChildren();
        log.info("lake.realm.init: {} -> {}", realms.getLake(), files);
        boot(realms, files);
    }

    public static void boot(RealmManager realms, FileObject[] files) throws Exception, PlatformException {
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                FileObject file = files[i];
                if (file.isFolder() && !file.getName().getBaseName().startsWith(".")) {
                    String realmId = file.getName().getBaseName() + ":";
                    log.debug("lake.realm.new: {}", realmId);
                    I_Realm realm = realms.newRealm(Values.iri(realmId));
                    boot(realm, file);
                }
            }
        }
    }

    public static AbstractLake boot(I_Realm realm, FileObject file) throws IOException {
        try (RepositoryConnection connection = realm.getRepository().getConnection()) {
            log.info("lake.realm.boot: {} @ {} x {}  facts", realm.getSelf(), file.getPath().toString(),
                    connection.size());
            BootstrapVFSLake loader = new BootstrapVFSLake(realm.getSelf().stringValue(), connection, true, true, true,
                    true);
            loader.deploy(file);
            log.info("lake.realm.deployed: {} x {} facts", realm.getSelf(), connection.size());
            return loader;
        }
    }
}
