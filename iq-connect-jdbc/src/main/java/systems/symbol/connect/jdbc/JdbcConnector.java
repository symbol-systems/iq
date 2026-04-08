package systems.symbol.connect.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import systems.symbol.connect.AbstractConnector;
import systems.symbol.connect.core.ConnectorState;
import systems.symbol.connect.jdbc.schema.JdbcR2RMLGenerator;
import systems.symbol.connect.jdbc.schema.JdbcSchemaIntrospector;
import systems.symbol.connect.jdbc.schema.JdbcSchemaIntrospector.TableMetadata;

/**
 * JDBC Connector: Introspects SQL database schemas and generates R2RML mappings.
 *
 * <p>
 * Lifecycle:
 * </p>
 * <ol>
 *   <li>Load config (JDBC URL, credentials, discovery options)</li>
 *   <li>Connect to database</li>
 *   <li>Introspect schema (tables, columns, keys)</li>
 *   <li>Generate R2RML mappings as RDF triples</li>
 *   <li>Store in named graph ({realm}/schemas/{connectionHash})</li>
 *   <li>Register virtual graphs</li>
 * </ol>
 *
 * <p>
 * The connector does NOT execute queries or translate SPARQL to SQL; that's handled
 * by {@link systems.symbol.rdf4j.fedx.JdbcRemoteSPARQLClient} in the federation layer.
 * </p>
 */
public class JdbcConnector extends AbstractConnector {

  private static final Logger LOG = Logger.getLogger(JdbcConnector.class.getName());

  private JdbcConnectorConfig config;
  private Connection jdbcConnection;
  private JdbcSchemaIntrospector introspector;
  private JdbcR2RMLGenerator r2rmlGenerator;

  @Override
  protected void initialize() throws Exception {
    LOG.log(Level.INFO, "Initializing JDBC Connector");

    // Load configuration from connector metadata
    loadConfig();

    // Establish JDBC connection
    openDatabaseConnection();

    // Initialize introspector and generator
    this.introspector = new JdbcSchemaIntrospector(
        jdbcConnection,
        config.getSchemas().isEmpty() ? null : config.getSchemas().get(0),
        null);

    this.r2rmlGenerator = new JdbcR2RMLGenerator(
        entityBaseUri(),
        entityNamespace());

    LOG.log(Level.INFO, "JDBC Connector initialized: {0}", config.getJdbcUrl());
  }

  @Override
  protected void doRefresh(RepositoryConnection repoConnection) throws Exception {
    LOG.log(Level.INFO, "Starting JDBC schema introspection");

    // 1. Introspect the database schema
    List<TableMetadata> tables = introspector.introspectTables();
    LOG.log(Level.INFO, "Introspected {0} tables", tables.size());

    // 2. Generate R2RML mappings as RDF
    Model r2rmlModel = r2rmlGenerator.generateR2RML(tables);
    LOG.log(Level.INFO, "Generated {0} R2RML triples", r2rmlModel.size());

    // 3. Store R2RML in named graph: {realm}/schemas/{connectionHash}
    IRI schemasGraphIri = createSchemasGraphIri();
    storeR2RMLGraphAsync(repoConnection, schemasGraphIri, r2rmlModel);

    // 4. Register virtual graphs for each discovered table
    registerVirtualGraphs(repoConnection, tables, schemasGraphIri);

    updateState(ConnectorState.ACTIVE);
    LOG.log(Level.INFO, "JDBC schema introspection complete");
  }

  @Override
  protected void doShutdown() throws Exception {
    LOG.log(Level.INFO, "Shutting down JDBC Connector");

    if (jdbcConnection != null && !jdbcConnection.isClosed()) {
      jdbcConnection.close();
    }

    updateState(ConnectorState.STOPPED);
  }

  /**
   * Load connector configuration from RDF metadata.
   *
   * <p>
   * In production, this would read from a configuration RDF graph or property file.
   * For now, we parse from a simple map.
   * </p>
   */
  private void loadConfig() throws Exception {
    // TODO: Load from connector metadata graph
    // For now, a placeholder that would be overridden by subclass or config
    this.config = new JdbcConnectorConfig.Builder()
        .jdbcUrl("jdbc:h2:mem:test")
        .user("sa")
        .password("")
        .autoDiscover(true)
        .build();

    LOG.log(Level.FINE, "Loaded JDBC config: {0}", config.getJdbcUrl());
  }

  /**
   * Open JDBC connection to the database.
   */
  private void openDatabaseConnection() throws SQLException {
    try {
      jdbcConnection = DriverManager.getConnection(
          config.getJdbcUrl(),
          config.getUser(),
          config.getPassword());

      jdbcConnection.setNetworkTimeout(
          java.util.concurrent.Executors.newScheduledThreadPool(1),
          config.getConnectionTimeout() * 1000);

      LOG.log(Level.INFO, "Connected to database: {0}", config.getJdbcUrl());
    } catch (SQLException e) {
      LOG.log(Level.SEVERE, "Failed to connect to database", e);
      throw e;
    }
  }

  /**
   * Create the named graph IRI where R2RML mappings will be stored.
   *
   * <p>
   * Format: {realm}/schemas/{connectionHash}
   * </p>
   */
  private IRI createSchemasGraphIri() {
    String hash = Integer.toHexString(config.getJdbcUrl().hashCode());
    return rdf4j().createIRI(realmIri() + "/schemas/jdbc-" + hash);
  }

  /**
   * Store R2RML model in the named graph (async, batched).
   */
  private void storeR2RMLGraphAsync(
      RepositoryConnection conn,
      IRI graphIri,
      Model r2rmlModel) throws Exception {
    // Clear existing triples in this graph
    conn.clear(graphIri);

    // Add new triples
    conn.add(r2rmlModel, graphIri);

    LOG.log(Level.INFO, "Stored R2RML graph: {0}", graphIri);
  }

  /**
   * Register each discovered table as a virtual graph instance in RDF.
   */
  private void registerVirtualGraphs(
      RepositoryConnection conn,
      List<TableMetadata> tables,
      IRI r2rmlGraphIri) throws Exception {

    for (TableMetadata table : tables) {
      String tableHash = Integer.toHexString(table.getTableName().hashCode());
      IRI vgIri = rdf4j().createIRI(realmIri() + "/virtual-graphs/jdbc-" + tableHash);

      // Create VirtualGraph instance RDF triples
      // (Would use ConnectorGraphModeller in production)
      IRI vgType = rdf4j().createIRI("urn:iq:virtual:VirtualGraph");
      conn.add(vgIri, org.eclipse.rdf4j.model.vocabulary.RDF.TYPE, vgType);

      IRI sourceTypeProperty = rdf4j().createIRI("urn:iq:virtual:sourceType");
      IRI jdbcSourceType = rdf4j().createIRI("urn:iq:virtual:JDBC");
      conn.add(vgIri, sourceTypeProperty, jdbcSourceType);

      IRI labelProperty = org.eclipse.rdf4j.model.vocabulary.RDFS.LABEL;
      conn.add(vgIri, labelProperty,
          rdf4j().createLiteral("JDBC: " + table.getTableName()));

      IRI r2rmlGraphProperty = rdf4j().createIRI("urn:iq:virtual:r2rmlGraph");
      conn.add(vgIri, r2rmlGraphProperty, r2rmlGraphIri);

      LOG.log(Level.FINE, "Registered virtual graph: {0}", vgIri);
    }

    LOG.log(Level.INFO, "Registered {0} virtual graphs", tables.size());
  }

  /** Get realm IRI. */
  private IRI realmIri() {
    // TODO: Get from connector metadata
    return rdf4j().createIRI("urn:iq:realm:default");
  }

  /** Get entity base URI. */
  private String entityBaseUri() {
    return "urn:iq:databases:";
  }

  /** Get entity namespace. */
  private String entityNamespace() {
    return entityBaseUri() + config.getJdbcUrl().hashCode() + ":";
  }

  /** RDF4J value factory shorthand. */
  private static org.eclipse.rdf4j.model.ValueFactory rdf4j() {
    return org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
  }
}
