/**
 * The Platform class represents the central component of the application.
 * It manages the knowledge base, provides access to repositories, and handles text and fact finders.
 * The class is a singleton, ensuring a single instance throughout the application.
 */
package systems.symbol.platform;

import com.auth0.jwt.JWTCreator;
import jakarta.inject.Singleton;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.TextFinder;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.SimpleKeyStore;
import systems.symbol.trust.generate.JWTGen;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import static systems.symbol.COMMONS.IQ;

@Singleton
public class Platform implements I_Self, I_StartStop, I_Keys {
    protected static final Logger log = LoggerFactory.getLogger(Platform.class);

    Workspace workspace;
    LRUCache<String, TextFinder> tfCache = new LRUCache<>(4);
    LRUCache<String, FactFinder> ffCache = new LRUCache<>(4);
    File cacheHome, importsHome, vaultHome;
    JWTGen jwtGen = new JWTGen();
    private SimpleKeyStore keysStore;

    /**
     * Constructs a Platform instance and initializes the knowledge base workspace.
     *
     * @throws IOException If an error occurs during workspace initialization.
     */
    public Platform() throws Exception {
        init(new File(IQ.toLowerCase()));
    }

    /**
     * Initializes the Platform with the specified home directory.
     *
     * @param home The home directory for the knowledge base.
     * @throws IOException If an error occurs during initialization.
     */
    private void init(File home) throws Exception {
        log.info("platform.init: {} @ {}", home.getAbsolutePath(), new Date());
        workspace = new Workspace(home);
        this.cacheHome = new File(workspace.getHome(), "cache");
        this.importsHome = new File(workspace.getHome(), "import");
        this.vaultHome = new File(workspace.getHome(), "vault");

        WorkspaceProvisioner provisioner = new WorkspaceProvisioner(workspace);
        provisioner.deploy(importsHome, true);
        this.keysStore = new SimpleKeyStore(vaultHome);
        log.info("platform.vault: {}", vaultHome.getAbsolutePath());

        // if (Validate.isUnGuarded())
        System.out.printf("** OWNER TOKEN ** \nexport JWT='%s'\n****\n", generateJWT());
    }

    public File getImportsHome() {
        return this.importsHome;
    }

    protected String generateJWT() throws Exception {
        String self = workspace.getSelf().stringValue();
        // expires in an hour
        JWTCreator.Builder jwtBuilder = jwtGen.generate(self, self, new String[] { self }, 3600, I_Self.name(),
                new String[] { I_Self.name() });
        return jwtGen.sign(jwtBuilder, keys());
    }

    public I_Secrets getSecrets() {
        return new EnvsAsSecrets();
    }

    public KeyPair keys() throws SecretsException {
        try {
            return keysStore.load();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SecretsException(e.getMessage());
        }
    }

    /**
     * Retrieves the Workspace associated with the Platform.
     *
     * @return The Workspace instance.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Retrieves a Repository based on the specified name.
     *
     * @param name The name of the Repository.
     * @return The Repository instance.
     */
    public Repository getRepository(String name) throws IOException {
        return this.workspace.alwaysGetRepository(name);
    }

    /**
     * Retrieves a TextFinder based on the specified key.
     *
     * @param key The key for the TextFinder.
     * @return The TextFinder instance.
     */
    public TextFinder getTextFinder(String key) {
        TextFinder finder = tfCache.get(key);
        if (finder != null)
            return finder;
        finder = new TextFinder(new File(this.cacheHome, "tf/" + key));
        tfCache.put(key, finder);
        return finder;
    }

    /**
     * Retrieves a FactFinder based on the specified key.
     *
     * @param key The key for the FactFinder.
     * @return The FactFinder instance.
     */
    public FactFinder getFactFinder(String key) {
        FactFinder finder = ffCache.get(key);
        if (finder != null)
            return finder;
        Repository repo = workspace.getCurrentRepository();
        finder = new FactFinder(repo, new File(this.cacheHome, "ff/" + key), null, 10, 0.8);
        ffCache.put(key, finder);
        return finder;
    }

    /**
     * Clears the caches (TextFinder and FactFinder).
     */
    public void cleanup() {
        ffCache.clear();
        tfCache.clear();
    }

    /**
     * Checks if the Platform is in a healthy state.
     *
     * @return True if the Platform is healthy, otherwise false.
     */
    public boolean isHealthy() {
        return workspace != null && workspace.getCurrentRepository().isInitialized();
    }

    /**
     * Shuts down all Repositories associated with the Platform/Workspace.
     */
    public void shutdown() {
        cleanup();
        workspace.shutdown();
    }

    /**
     * Retrieves the identity of the Platform.
     *
     * @return The IRI representing the identity of the Platform.
     */
    @Override
    public IRI getSelf() {
        return workspace.getSelf();
    }

    public void boot() {
    }

    public void start() {
    }

    @Override
    public void stop() {
        workspace.shutdown();
    }
}
