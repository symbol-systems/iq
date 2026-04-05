package systems.symbol.lake;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class BootstrapRepository extends RepositoryWrapper {
private static final Logger log = LoggerFactory.getLogger(BootstrapRepository.class);
boolean loaded = false;

public BootstrapRepository() throws IOException {
setDelegate(new SailRepository(new MemoryStore()));
}

public BootstrapRepository(File root, String context) throws IOException {
this();
load(root, context);
}

public BootstrapRepository(Repository delegated) {
setDelegate(delegated);
}

public IRI load(File assetsRoot, String context) throws IOException {
return load(assetsRoot, context, true, true);
}

public IRI load(File assetsRoot, String context, boolean force, boolean deployRDF) throws IOException {
log.info("bootstrap.load: {} -> {}", assetsRoot, context);
log.info("bootstrap.load.options: force:{}, rdf: {}", force, deployRDF);
try (RepositoryConnection connection = getConnection()) {
// Prefer VFS deployment path for lake data ingestion
try {
FileSystemManager vfs = VFS.getManager();
FileObject root = vfs.resolveFile(assetsRoot.getAbsolutePath());
BootstrapVFSLake loader = new BootstrapVFSLake(context, connection, force, deployRDF, true, true);
loader.deploy(root);
loaded = true;
log.info("bootstrap.load.done (vfs): {} @ {}", assetsRoot.getAbsolutePath(), loader.getSelf());
return loader.getSelf();
} catch (Exception vfsEx) {
log.warn("bootstrap.load.vfs.failed: {}", vfsEx.getMessage(), vfsEx);
BootstrapLake loader = new BootstrapLake(context, connection, force, deployRDF, true, true);
loader.deploy(assetsRoot);
loaded = true;
log.info("bootstrap.load.done (file): {} @ {}", assetsRoot.getAbsolutePath(), loader.getSelf());
return loader.getSelf();
}
}
}

public IRI load(File src, IRI context) throws IOException {
return load(src, context.stringValue(), true, true);
}

public TripleSource getTripleSource() {
return getTripleSource(true);
}

public TripleSource getTripleSource(boolean inferred) {
try (RepositoryConnection connection = this.getConnection()) {
return new RepositoryTripleSource(connection, inferred);
}
}
}
