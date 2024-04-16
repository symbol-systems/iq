package systems.symbol.cli;

import systems.symbol.model.I_Self;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.platform.Workspace;
import systems.symbol.string.PrettyString;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;


public class CLIContext implements I_Self {
    protected static final Logger log = LoggerFactory.getLogger(CLIContext.class);
    public static final String CODENAME = COMMONS.CODENAME;
    Workspace workspace;
    public File home, backups, assets, www_docs; // , kbms, onto;
    final long timestamp = System.currentTimeMillis();
    Repository repository;

    public CLIContext(File home) throws IOException {
        this.home = home;
        this.init();
    }

    public void init() throws IOException {
        workspace = new Workspace(home);
        this.repository = workspace.getCurrentRepository();
        log.info("iq.cli.workspace: " + workspace.getIdentity() +" @ "+(this.repository!=null?this.repository.getDataDir().getAbsolutePath():"n/a"));
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
        System.out.println(text);
    }

    public boolean isInitialized() {
        return (this.repository!=null && this.repository.isInitialized());
    }

    public void close() {
        if (this.repository==null) {
            log.warn("iq.cli.no-workspace");
            return;
        }
        this.repository.shutDown();
        log.debug("iq.cli.closed");
    }

    public File getAssetsHome() {
        return this.assets;
    }
    public IQ newIQBase() {
        return new IQConnection(getSelf(), getRepository().getConnection());
    }

    public boolean isStale(File file) {
        if (!file.exists())
            return true;
        return timestamp >= (file.lastModified()); // last 60 seconds
    }

    @Override
    public IRI getSelf() {
        return workspace == null ? null : workspace.getIdentity();
    }

    public String toString() {
        return getSelf() + " on " + new Date();
    }

    public Repository getRepository() {
        return workspace.getCurrentRepository();
    }

}
