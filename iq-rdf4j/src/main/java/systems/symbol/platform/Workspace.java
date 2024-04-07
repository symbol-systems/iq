package systems.symbol.platform;
/**
 * Manages the RDF4J repositories within a workspace.
 */

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class Workspace {
    private final Logger log = LoggerFactory.getLogger(getClass());
    final String TYPEOF_REPOSITORY = "http://www.openrdf.org/config/repository#Repository";
    final String ACTIVE_REPO_PROP = "current.repo";
    final String STORE_PROP = "current.store";
    final String NS_PROP = "current.ns";
    private String DEFAULT_NS = "urn:fact:default:";

    RepositoryManager manager;
    Map<String, Repository> repositories = new HashMap<>();
    File home, propsFile;
    Properties properties = new Properties();
    String repoTemplatePath = "kbms/";
    IRI ns;
    /**
     * Constructs a Workspace with the specified home directory.
     *
     * @param home The home directory for the workspace.
     * @throws IOException If there is an issue with the workspace initialization.
     */
    public Workspace(File home) throws IOException {
        if (!home.exists()) {
            if (!home.mkdirs()) throw new IOException("failed to create: "+home.getAbsolutePath());
        }
        if (!home.isDirectory())
            throw new IOException("not a folder: " + home.getAbsolutePath());
        this.home = home;
        this.manager = new LocalRepositoryManager(home);
        propsFile = new File(home, "workspace.properties");
        initialize();
    }

    private void initialize() throws IOException {
        this.DEFAULT_NS = System.getenv("MY_NS") == null?DEFAULT_NS:System.getenv("MY_NS");
        if (!propsFile.exists()) {
            initProperties();
            save();
            log.info("repo.initialized: {}", propsFile.getAbsolutePath());
            this.alwaysGetRepository(getCurrentRepositoryName());
        }
        properties.load(new FileInputStream(propsFile));
        this.ns = SimpleValueFactory.getInstance().createIRI(properties.getProperty(NS_PROP, DEFAULT_NS));
        log.info("workspace.initialized: {} -> {} @ {}", getIdentity(), this.repositories, new Date());
    }

    private void initProperties() {
        String today = "" + System.currentTimeMillis();
        properties.put("iq.created", today);
        properties.put("iq.modified", today);
        properties.put(NS_PROP, DEFAULT_NS);
        properties.put(ACTIVE_REPO_PROP, "default");
        properties.put(STORE_PROP, "default");
        this.ns = SimpleValueFactory.getInstance().createIRI(DEFAULT_NS);
    }

    public File getHome() {
        return home;
    }

    public String getOwnerNamespace() {
        return properties.getProperty(NS_PROP, DEFAULT_NS);
    }

    public String getCurrentRepositoryName() {
        return properties.getProperty(ACTIVE_REPO_PROP);
    }

    public String getStoreType() {
        return properties.getProperty(STORE_PROP);
    }

    public IRI getIdentity() {
        return ns;
    }

    public String getProperty(String key) {
        return this.properties.getProperty(key, "");
    }

    public void setProperty(String key, String value) throws IOException {
        this.properties.setProperty(key, value);
        this.save();
    }

//    public String getCurrentStore() {
//        return properties.getProperty(ACTIVE_REPO_PROP, "default");
//    }
    public void save() throws IOException {
        propsFile.getParentFile().mkdirs();
        properties.store(new FileOutputStream(propsFile), "last saved on " + propsFile.getName());
        log.debug("repository.props.saved: {}", propsFile.getAbsolutePath());
    }

    /**
     * create a repository - i.e: should always return a repository
     * @param id            unique name of the repository
     * @param storeType     the config type
     * @return              an RDF4J Repository
     * @throws IOException  oops
     */
    public Repository create(String id, String storeType) throws IOException {
        Map<String,String> ctx = new HashMap<>();
        ctx.put("id", id);
        RepositoryConfig config = newRepoConfig(storeType, ctx);
        return create(config);
    }

        /**
         * Create a new repository based on a RepositoryConfig
         *
         * @param config the RepositoryConfig for a new Repository
         * @throws IOException repository failed to create
         */
    protected Repository create(RepositoryConfig config) throws IOException {
        try {
            this.manager.addRepositoryConfig(config);

            log.info("repository.created: {} -> {}", config.getID(), config.getTitle());
        } catch (RepositoryReadOnlyException e) {
            this.manager.addRepositoryConfig(config);
            log.warn("repository.readonly.created");
        }
        Repository repository = manager.getRepository(config.getID());
        log.debug("repository.get: {}", repository);
        if (!repository.isInitialized())
            repository.init();
        return repository;
    }

    /**
     * Create a new RepositoryConfig based on a template and parameters
     *
     * @param storeType     Name of a TTL template in ./resources/kbms/
     * @param ctx          the key/values for template interpolation
     */
    protected RepositoryConfig newRepoConfig(final String storeType, Map<String, String> ctx) throws IOException {
        String resourcePath = repoTemplatePath + storeType + ".ttl";
        log.info("repository.config: {}", resourcePath);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        return toRepoConfig(inputStream, ctx);
    }

    /**
     * Create a new repository based on a template
     *
     * @param in  The InputStream containing a TTL template for repository
     * @param ctx the key/values for template interpolation
     */
    public RepositoryConfig toRepoConfig(final InputStream in, Map<String, String> ctx) throws IOException {
        String template = IOUtil.readString(in);
        return toRepoConfig(template, ctx);
    }

    /**
     * Create a new repository based on a template
     *
     * @param template the TTL template for repository
     * @param ctx      the key/values for template interpolation
     */
    public RepositoryConfig toRepoConfig(final String template, Map<String, String> ctx) {
        try {
            final ConfigTemplate configTemplate = new ConfigTemplate(template);
//            log.info("repository.config.rendered: {} -> {}", template, ctx);
            final String configString = configTemplate.render(ctx);
            log.debug("repository.config.rendered: {}", configString);
            final Model graph = new LinkedHashModel();

            ValueFactory vf = SimpleValueFactory.getInstance();
            final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, vf);
            rdfParser.setRDFHandler(new StatementCollector(graph));
            rdfParser.parse(new StringReader(configString), getIdentity().toString());

            Resource repositoryType = vf.createIRI(TYPEOF_REPOSITORY);
            final Resource repositoryNode = Models
                    .subject(graph.filter(null, RDF.TYPE, repositoryType))
                    .orElseThrow(() -> new RepositoryConfigException(
                            "missing type of " + repositoryType.stringValue()));

            final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
            repConfig.validate();
            return repConfig;

        } catch (Exception e) {
            log.error("repository.create.failed", e);
        }
        return null;
    }

    /**
     * Create input stream from a template file in the specified file directory. If
     * the file cannot be found, try to
     * read it from the embedded java resources instead.
     *
     * @param templateName     name of the template
     * @param templateFileName template file name
     * @param templatesDir     template directory
     * @param templateFile     template file
     * @return input stream of the template
     * @throws FileNotFoundException
     */
    public InputStream createTemplateStream(final String templateName, final String templateFileName,
                                            final File templatesDir, final File templateFile) throws FileNotFoundException {
        InputStream templateStream = null;
        if (templateFile.exists()) {
            if (templateFile.canRead()) {
                templateStream = new FileInputStream(templateFile);
            } else {
                log.error("Not allowed to read template file: " + templateFile);
            }
        } else {
            // Try class path for built-ins
            templateStream = RepositoryConfig.class.getResourceAsStream(templateFileName);
            if (templateStream == null) {
                log.error("No template called " + templateName + " found in " + templatesDir);
            }
        }
        return templateStream;
    }

    public Repository alwaysGetRepository(String id) throws IOException {
        log.info("workspace.repo.all: {} -> {} -> {} --> {}", id, manager.getRepositoryIDs(), manager.hasRepositoryConfig(id), repositories.keySet());
        Repository repository = getRepository(id);
        if (repository!=null) {
            log.info("workspace.repo.found: {}", id);
            return repository;
        }
        Repository new_repository = create(id, "default");
        if (new_repository==null) {
            log.warn("workspace.repo.always.failed: {}", id);
            return null;
        }
        log.info("workspace.repo.always: {}", id);
        repositories.put(id, new_repository);
        return new_repository;
    }

    public Repository getRepository(String id) {
        Repository repository = repositories.get(id);
        if (repository!=null) {
            log.info("workspace.repo.cached: {}", id);
            return repository;
        }
        try {
            Repository m_repository = manager.getRepository(id);
            if (m_repository == null) {
                log.warn("workspace.repo.missing: {}", id);
                return null;
            }
            log.info("workspace.repo.exists: {}", id);
            repositories.put(id, m_repository);
            return m_repository;
        } catch (RepositoryConfigException e) {
            log.error("workspace.repo.config: {} -> {}", id, e.getMessage());
        } catch (RepositoryException e) {
            log.error("workspace.repo.error: {} -> {}", id, e.getMessage());
        }
        return null;
    }

    public Repository getCurrentRepository() {
        String id = properties.getProperty(ACTIVE_REPO_PROP, "default");
        Repository repository = null;
        try {
            return alwaysGetRepository(id);
        } catch (IOException e) {
            return null;
        }
    }

    public void setCurrentRepository(String id) throws IOException {
        properties.put(ACTIVE_REPO_PROP, id);
        save();
    }

    public void shutdown() {
        for(String key: repositories.keySet()) {
            repositories.get(key).shutDown();
        }
    }
}
