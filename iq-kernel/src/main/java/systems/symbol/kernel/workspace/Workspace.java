package systems.symbol.kernel.workspace;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.platform.I_Self;

/**
 * Manages the {@code .iq/} workspace directory tree and provides a stable
 * {@link I_Self} IRI for the current runtime.
 *
 * <p>This class is intentionally free of RDF4J repository / SPARQL dependencies
 * so it can be used before any repository is opened. Repository initialisation
 * is performed by {@code iq-platform}'s {@code RealmManager}, which receives
 * the resolved {@link #getSelf()} IRI and home directory from here.
 *
 * <p>The workspace IRI is derived in the following order:
 * <ol>
 *   <li>Environment variable {@code MY_IQ} (e.g. {@code acme:})</li>
 *   <li>Basename of {@code home} directory (e.g. {@code file:///home/user/.iq})</li>
 *   <li>Default fallback: {@code iq:}</li>
 * </ol>
 *
 * <p>Directory layout (mirrors the existing {@code .iq/} conventions):
 * <pre>
 * home/
 *   assets/
 *   backups/
 *   public/
 *   repositories/← RDF4J repository data (managed by iq-platform)
 *   vault/   ← VFS password vault (managed by iq-trusted)
 * </pre>
 *
 * <p>All file operations use Apache Commons VFS2 for unified virtual filesystem access.
 */
public class Workspace implements I_Self {

private final FileObject home;
private final IRI  self;

/** Sub-directories created automatically on construction. */
public final FileObject assets;
public final FileObject backups;
public final FileObject pub;

/**
 * Create a workspace from a FileObject (VFS).
 * @param home the home directory as FileObject
 * @throws FileSystemException if directories cannot be created
 */
public Workspace(FileObject home) throws FileSystemException {
this.home= home;
this.self= resolveIRI(home);
this.assets  = mkdir(home.resolveFile("assets"));
this.backups = mkdir(home.resolveFile("backups"));
this.pub = mkdir(home.resolveFile("public"));
}

@Override
public IRI getSelf() { return self; }

public FileObject getHome(){ return home; }
public FileObject getAssets()  { return assets; }
public FileObject getBackups() { return backups; }
public FileObject getPublic()  { return pub; }

/**
 * Type of backing store declared by the workspace (e.g. {@code "native"},
 * {@code "memory"}). Currently derived from a convention file; defaults to
 * {@code "native"}.
 */
public String getStoreType() {
try {
FileObject typeFile = home.resolveFile("store-type");
if (!typeFile.exists()) return "native";
String s = new String(typeFile.getContent().getByteArray()).trim();
return s.isEmpty() ? "native" : s;
} catch (Exception e) {
return "native";
}
}

/** {@code true} if the home directory exists and is a directory. */
public boolean isInitialized() {
try {
return home.exists() && home.getType().hasContent();
} catch (FileSystemException e) {
return false;
}
}

@Override
public String toString() {
try {
return "Workspace[" + self + " @ " + home.getURL() + "]";
} catch (Exception e) {
return "Workspace[" + self + " @ <unknown>]";
}
}

/* ── private helpers ──────────────────────────────────────────────────── */

private static IRI resolveIRI(FileObject home) {
String env = System.getenv("MY_IQ");
if (env != null && !env.isBlank()) {
return SimpleValueFactory.getInstance().createIRI(
env.endsWith(":") ? env : env + ":");
}
try {
return SimpleValueFactory.getInstance().createIRI(
home.getURL().toString());
} catch (Exception e) {
// Fallback to default IRI if unable to resolve home URL
return SimpleValueFactory.getInstance().createIRI("iq:");
}
}

private static FileObject mkdir(FileObject dir) throws FileSystemException {
if (!dir.exists()) {
dir.createFolder();
}
return dir;
}
}
