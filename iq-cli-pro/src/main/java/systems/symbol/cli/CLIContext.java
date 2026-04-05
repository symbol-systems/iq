package systems.symbol.cli;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.COMMONS;
import systems.symbol.kernel.I_Kernel;
import systems.symbol.kernel.KernelBuilder;
import systems.symbol.kernel.KernelContext;
import systems.symbol.platform.I_Self;
import systems.symbol.platform.Workspace;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.string.PrettyString;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * CLI context for IQ Pro operations (extended CLI for agents, models, and complex workflows).
 * 
 * Extends the core CLI context with:
 * - Agent runtime context (actor state, fleet management)
 * - Model provider selection (OpenAI, Groq, custom)
 * - Persistent configuration storage (set-default handlers)
 * - Cost tracking via ServerRuntimeManager
 * 
 * Provides unified access to:
 * - KernelContext (auth, audit, quota)
 * - Workspace (home directory, backups, assets)
 * - Repository (RDF store, models, agents)
 * - Display (console output, error handling)
 * 
 * @author Symbol Systems
 * @version 0.94+pro
 */
public class CLIContext implements I_Self {
protected static final Logger log = LoggerFactory.getLogger(CLIContext.class);
public static final String CODENAME = COMMONS.IQ;

private final I_Kernel kernel;
private final KernelContext kernelContext;
Workspace workspace;
public File home, backups, assets, www_docs;
final long timestamp = System.currentTimeMillis();
Repository repository;

/**
 * Initialize CLI context from existing kernel.
 * Useful for embedded CLI (e.g., Quarkus CLI endpoint).
 * 
 * @param kernel I_Kernel with context and home
 * @throws IOException if unable to initialize workspace
 */
public CLIContext(I_Kernel kernel) throws IOException {
this.kernel = kernel;
this.kernelContext = kernel.getContext();
this.home = kernelContext.getHome();
this.init();
}

/**
 * Initialize CLI context from home directory.
 * Starts kernel if not already running.
 * 
 * @param home workspace home directory
 * @throws Exception if unable to build/start kernel
 */
public CLIContext(File home) throws Exception {
I_Kernel kernel = KernelBuilder.create().withHome(home).build();
kernel.start();
this.kernel = kernel;
this.kernelContext = kernel.getContext();
this.home = kernelContext.getHome();
this.init();
}

/**
 * Initialize workspace directories and repository.
 * Called during construction; handles setup of backups, assets, etc.
 * 
 * @throws IOException if unable to create directories or access repository
 */
public void init() throws IOException {
workspace = new Workspace(home);
this.repository = workspace.getCurrentRepository();
log.info("iq.cli.workspace: " + workspace.getSelf() +" @ "+(this.repository!=null?"repository initialized":"n/a"));
if (this.repository == null || !this.isInitialized()) {
log.info("no repository: "+this.repository);
}
String path = PrettyString.sanitize(workspace.getStoreType());
backups = new File(home, "backups" + File.separator + path);
www_docs = new File(home, "public" + File.separator + path);
assets = new File(home, "assets" + File.separator + path);
backups.mkdirs();
www_docs.mkdirs();
assets.mkdirs();
}

private systems.symbol.io.I_Display display = systems.symbol.io.ConsoleDisplay.getInstance();

/**
 * Display message to standard output.
 * 
 * @param text message to display
 */
public void display(String text) {
display.out(text);
}

/**
 * Display error message to standard error.
 * 
 * @param text error message
 */
public void displayError(String text) {
display.err(text);
}

/**
 * Get the display interface for output.
 * Allows customization of output handling (console, GUI, remote, etc).
 * 
 * @return I_Display implementation
 */
public systems.symbol.io.I_Display getDisplay() {
return display;
}

/**
 * Set custom display interface.
 * Useful for testing, GUI embedding, or remote display.
 * 
 * @param display custom display implementation
 */
public void setDisplay(systems.symbol.io.I_Display display) {
if (display != null) {
this.display = display;
}
}

/**
 * Check if IQ workspace is initialized.
 * An initialized workspace has:
 * - Repository initialized (RDF store ready)
 * - Kernel context established
 * - Workspace directories created
 * 
 * @return true if workspace is ready for commands
 */
public boolean isInitialized() {
return (this.repository!=null && this.repository.isInitialized());
}

/**
 * Shutdown CLI context.
 * Closes repository and stops kernel gracefully.
 * Should be called in finally block to ensure cleanup.
 */
public void close() {
if (this.repository != null) {
this.repository.shutDown();
log.debug("iq.cli.closed repository");
} else {
log.warn("iq.cli.no-workspace");
}
if (kernel != null) {
try {
kernel.stop();
} catch (Exception e) {
log.warn("iq.cli.kernel.stop.failed", e);
}
}
}

/**
 * Get KernelContext for auth, audit, quota enforcement.
 * The kernel context enforces:
 * - JWT token validation (auth middleware)
 * - Realm isolation (SPARQL FILTER injection)
 * - Audit logging (all mutations to mcp:audit)
 * - Rate limiting (quota enforcement per principal)
 * 
 * @return KernelContext for this session
 */
public KernelContext getKernelContext() {
return this.kernelContext;
}

/**
 * Get assets directory (models, ontologies, templates).
 * 
 * @return File pointing to assets directory
 */
public File getAssetsHome() {
try {
return kernelContext != null ? kernelContext.getAssets() : this.assets;
} catch (NoSuchMethodError e) {
return this.assets;
}
}

/**
 * Get backups directory.
 * 
 * @return File pointing to backups directory
 */
public File getBackupsHome() {
try {
return kernelContext != null ? kernelContext.getBackups() : this.backups;
} catch (NoSuchMethodError e) {
return this.backups;
}
}

/**
 * Get public documentation directory (for HTML, API docs, etc).
 * 
 * @return File pointing to public directory
 */
public File getPublicHome() {
try {
return kernelContext != null ? kernelContext.getPublic() : this.www_docs;
} catch (NoSuchMethodError e) {
return this.www_docs;
}
}

/**
 * Check if kernel is initialized.
 * Used by commands that need kernel availability.
 * 
 * @return true if kernel context is ready
 */
public boolean isKernelInitialized() {
try {
return kernelContext != null && kernelContext.isInitialized();
} catch (NoSuchMethodError e) {
return kernelContext != null && kernelContext.getHome() != null && kernelContext.getHome().exists();
}
}

/**
 * Create new IQ data source connection for RDF operations.
 * 
 * Returns an IQStore (RDF4J connection wrapper) for executing:
 * - SPARQL queries
 * - RDF graph operations
 * - Model discovery
 * - Agent queries
 * 
 * Caller must close connection when done.
 * 
 * @return IQStore for RDF operations
 */
public IQStore newIQBase() {
return new IQConnection(getSelf(), getRepository().getConnection());
}

/**
 * Check if file has been modified since context was created.
 * Useful for cache invalidation.
 * 
 * @param file file to check
 * @return true if file is stale (modified before context creation)
 */
public boolean isStale(File file) {
if (!file.exists())
return true;
return timestamp >= (file.lastModified());
}

/**
 * Get the IRI of the current realm/workspace.
 * 
 * @return IRI of the workspace
 */
@Override
public IRI getSelf() {
return workspace == null ? null : workspace.getSelf();
}

@Override
public String toString() {
return getSelf() + " on " + new Date();
}

/**
 * Get the underlying RDF repository for direct access.
 * 
 * @return RDF4J Repository
 */
public Repository getRepository() {
return workspace.getCurrentRepository();
}

/**
 * Recover workspace from backup.
 * Restores all RDF data from backup directory.
 * 
 * @throws IllegalStateException if workspace not initialized
 * @throws IOException if backup files not found or unreadable
 */
public void recover() throws IOException {
if (!isInitialized()) {
throw new IllegalStateException("IQ workspace is not initialized");
}
if (backups == null || !backups.exists()) {
throw new IOException("No backups directory found at " + (backups == null ? "null" : backups.getAbsolutePath()));
}
systems.symbol.io.ImportExport.restore(this);
}
}
