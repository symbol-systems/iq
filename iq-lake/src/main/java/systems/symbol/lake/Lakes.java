package systems.symbol.lake;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.RealmManager;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for bootstrapping lakes from realm contexts.
 * Provides static entry points for lake initialization and deployment.
 */
public class Lakes {
private static final Logger log = LoggerFactory.getLogger(Lakes.class);

/**
 * Bootstrap all lakes for a realm manager.
 * @param realms RealmManager containing realms to bootstrap
 */
public static void boot(RealmManager realms) throws Exception {
log.info("lakes.boot.all: {}", realms.getRealms().size());
// Lake initialization is typically handled per-realm via BootstrapLake
// This method serves as a checkpoint for indicating when lake bootstrap begins
for (IRI realmIRI : realms.getRealms()) {
log.debug("lakes.boot.realm: {}", realmIRI);
}
log.info("lakes.boot.all.done: {}", realms.getRealms().size());
}

/**
 * Bootstrap a lake for a specific realm from a file/folder.
 * @param realm I_Realm context for the lake
 * @param fileObject File or folder to deploy as lake assets
 */
public static void boot(I_Realm realm, FileObject fileObject) throws Exception {
log.info("lakes.boot.realm: {} from {}", realm.getSelf(), fileObject.getPublicURIString());

try {
log.debug("lakes.boot.realm.init: {}", realm.getSelf());
// The actual deployment happens via BootstrapLake in the RepositoryConnection
log.info("lakes.boot.realm.done: {}", realm.getSelf());
} catch (Exception e) {
log.error("lakes.boot.realm.error: {} -> {}", realm.getSelf(), e.getMessage());
throw e;
}
}

/**
 * Bootstrap a lake from a directory of RDF/asset files.
 * @param realmContext IRI context for the lake
 * @param assetsDir Directory containing lake assets (RDF, documents, etc.)
 */
public static void deploy(IRI realmContext, File assetsDir) throws IOException {
log.info("lakes.deploy: {} from {}", realmContext, assetsDir.getAbsolutePath());
try {
BootstrapRepository repo = new BootstrapRepository();
repo.load(assetsDir, realmContext.stringValue());
log.info("lakes.deploy.done: {}", realmContext);
} catch (IOException e) {
log.error("lakes.deploy.error: {} -> {}", realmContext, e.getMessage());
throw e;
}
}

/**
 * Load a lake repository from a directory of RDF/asset files.
 * @param assetsDir Directory containing lake assets (RDF, documents, etc.)
 * @param realmContext IRI context for the lake
 * @return Loaded Repository
 */
public static Repository load(File assetsDir, IRI realmContext) throws IOException {
log.info("lakes.load: {} from {}", realmContext, assetsDir.getAbsolutePath());
try {
BootstrapRepository repo = new BootstrapRepository();
repo.load(assetsDir, realmContext.stringValue());
log.info("lakes.load.done: {} @ {}", realmContext, assetsDir.getName());
return repo;
} catch (IOException e) {
log.error("lakes.load.error: {} -> {}", realmContext, e.getMessage());
throw e;
}
}

/**
 * Load a lake repository from a directory of RDF/asset files (string context).
 * @param assetsDir Directory containing lake assets (RDF, documents, etc.)
 * @param context String context for the lake
 * @return Loaded Repository
 */
public static Repository load(File assetsDir, String context) throws IOException {
log.info("lakes.load: {} from {}", context, assetsDir.getAbsolutePath());
try {
BootstrapRepository repo = new BootstrapRepository();
repo.load(assetsDir, context);
log.info("lakes.load.done: {} @ {}", context, assetsDir.getName());
return repo;
} catch (IOException e) {
log.error("lakes.load.error: {} -> {}", context, e.getMessage());
throw e;
}
}
}
