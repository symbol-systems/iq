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


public class CLIContext implements I_Self {
    protected static final Logger log = LoggerFactory.getLogger(CLIContext.class);
    public static final String CODENAME = COMMONS.IQ;

    private final I_Kernel kernel;
    private final KernelContext kernelContext;
    Workspace workspace;
    public File home, backups, assets, www_docs; // , kbms, onto;
    final long timestamp = System.currentTimeMillis();
    Repository repository;

    public CLIContext(I_Kernel kernel) throws IOException {
        this.kernel = kernel;
        this.kernelContext = kernel.getContext();
        this.home = kernelContext.getHome();
        this.init();
    }

    public CLIContext(File home) throws Exception {
        I_Kernel kernel = KernelBuilder.create().withHome(home).build();
        kernel.start();
        this.kernel = kernel;
        this.kernelContext = kernel.getContext();
        this.home = kernelContext.getHome();
        this.init();
    }

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

    public void display(String text) {
        log.info(text);
    }

    public boolean isInitialized() {
        return (this.repository!=null && this.repository.isInitialized());
    }

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

    public KernelContext getKernelContext() {
        return this.kernelContext;
    }

    public File getAssetsHome() {
        try {
            return kernelContext != null ? kernelContext.getAssets() : this.assets;
        } catch (NoSuchMethodError e) {
            return this.assets;
        }
    }

    public File getBackupsHome() {
        try {
            return kernelContext != null ? kernelContext.getBackups() : this.backups;
        } catch (NoSuchMethodError e) {
            return this.backups;
        }
    }

    public File getPublicHome() {
        try {
            return kernelContext != null ? kernelContext.getPublic() : this.www_docs;
        } catch (NoSuchMethodError e) {
            return this.www_docs;
        }
    }

    public boolean isKernelInitialized() {
        try {
            return kernelContext != null && kernelContext.isInitialized();
        } catch (NoSuchMethodError e) {
            return kernelContext != null && kernelContext.getHome() != null && kernelContext.getHome().exists();
        }
    }

    public IQStore newIQBase() {
        return new IQConnection(getSelf(), getRepository().getConnection());
    }

    public boolean isStale(File file) {
        if (!file.exists())
            return true;
        return timestamp >= (file.lastModified()); // last 60 seconds
    }

    @Override
    public IRI getSelf() {
        return workspace == null ? null : workspace.getSelf();
    }

    public String toString() {
        return getSelf() + " on " + new Date();
    }

    public Repository getRepository() {
        return workspace.getCurrentRepository();
    }

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
