package systems.symbol.platform;

import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class WorkspaceProvisioner {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceProvisioner.class);

    Workspace workspace;
    public WorkspaceProvisioner(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Identify which repositories are configured to auto-deploy
     * Each repository has an `iq.import.{repo}` property that includes the folder to import from
     * Each file within the asset folder is imported (supports triples & text content).
     *
     * @param name The name of repository to import.
     * @throws IOException If an error occurs during the import process.
     */
    public void deployConfigured(String name) throws IOException {
        String importPath = this.workspace.getProperty("import." + name);
        if (importPath.isEmpty()) return;
        deploy(name, new File(importPath), false);
    }

    /**
     * Imports all repositories from the specified directory.
     * Each repository has an assets sub-folder named after it.
     * Each file within the asset folder is imported (supports triples & text content).
     *
     * @param repoImportHome The directory containing repositories to import.
     * @param ensureRepository Flag to indicate if repositories should be ensured.
     * @throws IOException If an error occurs during the import process.
     */
    public void deploy(File repoImportHome, boolean ensureRepository) throws IOException {
        if (!repoImportHome.exists()) {
            if (ensureRepository) repoImportHome.mkdirs();
            log.warn("workspace.deploy.skip: {}", repoImportHome.getAbsolutePath());
            return;
        }
        // deploy from the ./import/ folder first
        File[] files = repoImportHome.listFiles();
        for (int i = 0; i < files.length; i++) {
            // each folder represents a repository (dot files are skipped)
            if (files[i].isDirectory()) {
                deploy(files[i].getName(), files[i], ensureRepository);
            }
        }
        // second, import based on property config
        for (String repo : workspace.manager.getRepositoryIDs()) {
            deployConfigured(repo);
        }
    }

    /**
     * Imports a repository from the specified directory.
     *
     * @param name              The name of the repository.
     * @param from              The source directory of the repository.
     * @param ensureRepository  Flag to indicate if the repository should be ensured.
     * @throws IOException      If an error occurs during the import process.
     */
    public void deploy(String name, File from, boolean ensureRepository) throws IOException {
        if (!from.exists() || from.getName().startsWith(".")) {
            from.mkdirs();
            log.warn("workspace.deploy.assets.skip: {}", from.getAbsolutePath());
            return;
        }
        Repository repository = ensureRepository ? this.workspace.alwaysGetRepository(name) : this.workspace.getRepository(name);
        if (repository == null) {
            log.warn("workspace.deploy.repository.missing: {}", name);
            return;
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            IQ iq = new IQConnection(workspace.getIdentity(), connection);
            BootstrapLoader loader = new BootstrapLoader(iq);
            log.info("workspace.deploy.from: {} -> {} @ {}", name, from.getAbsolutePath(), iq.getSelf());
            loader.deploy(from);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        repository.shutDown();
    }
}
