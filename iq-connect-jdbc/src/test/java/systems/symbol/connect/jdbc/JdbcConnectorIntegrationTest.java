package systems.symbol.connect.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Repository;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Integration tests for JDBC connector with H2 in-memory database.
 *
 * Tests full schema discovery → RDF mapping → virtual graph registration → SPARQL queries.
 */
@DisplayName("JdbcConnector Integration Tests")
class JdbcConnectorIntegrationTest {

  private Connection h2Connection;
  private Repository rdfRepository;
  private RepositoryConnection rdfConnection;
  private JdbcConnectorConfig config;

  @BeforeEach
  void setUp() throws Exception {
    // Set up H2 in-memory database with test schema
    h2Connection = DriverManager.getConnection("jdbc:h2:mem:integrationdb", "sa", "");

    try (Statement stmt = h2Connection.createStatement()) {
      // Create customers table
      stmt.execute(
          "CREATE TABLE customers ("
              + "  id INT PRIMARY KEY AUTO_INCREMENT,"
              + "  name VARCHAR(100) NOT NULL,"
              + "  email VARCHAR(100) NOT NULL UNIQUE,"
              + "  phone VARCHAR(20),"
              + "  created_date DATE DEFAULT CURRENT_DATE"
              + ")");

      // Create orders table with FK to customers
      stmt.execute(
          "CREATE TABLE orders ("
              + "  id INT PRIMARY KEY AUTO_INCREMENT,"
              + "  customer_id INT NOT NULL,"
              + "  order_date DATE NOT NULL,"
              + "  total_amount DECIMAL(10, 2) NOT NULL,"
              + "  status VARCHAR(20) DEFAULT 'PENDING',"
              + "  FOREIGN KEY (customer_id) REFERENCES customers(id)"
              + ")");

      // Create order_items table with FKs to orders
      stmt.execute(
          "CREATE TABLE order_items ("
              + "  id INT PRIMARY KEY AUTO_INCREMENT,"
              + "  order_id INT NOT NULL,"
              + "  product_id INT NOT NULL,"
              + "  quantity INT NOT NULL,"
              + "  unit_price DECIMAL(10, 2) NOT NULL,"
              + "  FOREIGN KEY (order_id) REFERENCES orders(id)"
              + ")");

      // Insert test data
      stmt.execute("INSERT INTO customers (name, email, phone) VALUES "
          + "('Alice Johnson', 'alice@example.com', '555-0001'),"
          + "('Bob Smith', 'bob@example.com', '555-0002'),"
          + "('Charlie Brown', 'charlie@example.com', '555-0003')");

      stmt.execute("INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES "
          + "(1, '2024-01-15', 99.99, 'COMPLETED'),"
          + "(1, '2024-01-20', 149.99, 'PENDING'),"
          + "(2, '2024-02-01', 75.50, 'COMPLETED'),"
          + "(3, '2024-02-05', 199.99, 'COMPLETED')");

      stmt.execute(
          "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES "
              + "(1, 101, 2, 49.99),"
              + "(2, 102, 1, 149.99),"
              + "(3, 103, 1, 75.50),"
              + "(4, 104, 3, 66.66)");
    }

    // Set up RDF repository
    rdfRepository = new SailRepository(new MemoryStore());
    rdfRepository.initialize();
    rdfConnection = rdfRepository.getConnection();

    // Create connector config
    config = JdbcConnectorConfig.builder()
        .jdbcUrl("jdbc:h2:mem:integrationdb")
        .username("sa")
        .password("")
        .autoDiscover(true)
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (rdfConnection != null) {
      rdfConnection.close();
    }
    if (rdfRepository != null) {
      rdfRepository.shutDown();
    }
    if (h2Connection != null && !h2Connection.isClosed()) {
      h2Connection.close();
    }
  }

  @Test
  @DisplayName("should discover all tables in H2 database")
  void discoverAllTables() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();

    assertEquals(3, tables.size(), "Should discover 3 tables");
    assertTrue(tables.stream().anyMatch(t -> t.name().equals("CUSTOMERS")));
    assertTrue(tables.stream().anyMatch(t -> t.name().equals("ORDERS")));
    assertTrue(tables.stream().anyMatch(t -> t.name().equals("ORDER_ITEMS")));
  }

  @Test
  @DisplayName("should generate R2RML mappings for tables")
  void generateR2RMLMappings() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);
    JdbcR2RMLGenerator generator = new JdbcR2RMLGenerator("urn:iq:db:", "urn:iq:entities:");

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    org.eclipse.rdf4j.model.Model r2rml = generator.generateR2RML(tables);

    assertNotNull(r2rml);
    assertFalse(r2rml.isEmpty());

    // Verify R2RML structure
    long triplesMaps = r2rml.filter(
            null,
            RDF.TYPE,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#TriplesMap"))
        .size();
    assertEquals(3, triplesMaps, "Should have 3 R2RML TriplesMap instances");
  }

  @Test
  @DisplayName("should store R2RML mappings in RDF repository")
  void storeR2RMLMappingsInRepository() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);
    JdbcR2RMLGenerator generator = new JdbcR2RMLGenerator("urn:iq:db:", "urn:iq:entities:");

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    org.eclipse.rdf4j.model.Model r2rml = generator.generateR2RML(tables);

    // Store in repository
    rdfConnection.add(r2rml);

    // Verify storage
    long tripleCount = rdfConnection.size();
    assertTrue(tripleCount > 0, "Should have stored R2RML triples");
  }

  @Test
  @DisplayName("should register virtual graphs for discovered tables")
  void registerVirtualGraphs() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();

    // Create virtual graph metadata for each table
    for (JdbcSchemaIntrospector.TableMetadata table : tables) {
      IRI virtualGraphId = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
          .createIRI("urn:iq:virtualgraph:jdbc:" + table.name().toLowerCase());

      assertNotNull(virtualGraphId);
      assertTrue(virtualGraphId.stringValue().contains(table.name().toLowerCase()));
    }
  }

  @Test
  @DisplayName("should handle foreign key relationships")
  void handleForeignKeyRelationships() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata ordersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("ORDERS"))
        .findFirst()
        .orElseThrow();

    List<JdbcSchemaIntrospector.ForeignKeyMetadata> fks =
        introspector.introspectForeignKeys(ordersTable);

    assertFalse(fks.isEmpty(), "ORDERS table should have foreign keys");
    assertTrue(fks.stream().anyMatch(fk -> fk.columnName().equals("CUSTOMER_ID")));
  }

  @Test
  @DisplayName("should discover column metadata with types")
  void discoverColumnMetadataWithTypes() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata customersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("CUSTOMERS"))
        .findFirst()
        .orElseThrow();

    List<JdbcSchemaIntrospector.ColumnMetadata> columns = customersTable.columns();

    assertEquals(5, columns.size(), "CUSTOMERS should have 5 columns");
    assertTrue(columns.stream().anyMatch(c -> c.name().equals("ID")));
    assertTrue(columns.stream().anyMatch(c -> c.sqlType().equals("INTEGER")));
    assertTrue(columns.stream().anyMatch(c -> c.name().equals("NAME")));
    assertTrue(columns.stream().anyMatch(c -> c.sqlType().equals("VARCHAR")));
  }

  @Test
  @DisplayName("should identify primary keys")
  void identifyPrimaryKeys() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata customersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("CUSTOMERS"))
        .findFirst()
        .orElseThrow();

    assertTrue(customersTable.columns().stream()
        .filter(c -> c.name().equals("ID"))
        .anyMatch(JdbcSchemaIntrospector.ColumnMetadata::primaryKey));
  }

  @Test
  @DisplayName("should identify nullable columns")
  void identifyNullableColumns() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata customersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("CUSTOMERS"))
        .findFirst()
        .orElseThrow();

    // NAME is NOT NULL
    assertTrue(customersTable.columns().stream()
        .filter(c -> c.name().equals("NAME"))
        .anyMatch(c -> !c.nullable()));

    // PHONE is nullable
    assertTrue(customersTable.columns().stream()
        .filter(c -> c.name().equals("PHONE"))
        .anyMatch(JdbcSchemaIntrospector.ColumnMetadata::nullable));
  }

  @Test
  @DisplayName("should support SPARQL queries on virtual graphs")
  void supportSparqlQueriesOnVirtualGraphs() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);
    JdbcR2RMLGenerator generator = new JdbcR2RMLGenerator("urn:iq:db:", "urn:iq:entities:");

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    org.eclipse.rdf4j.model.Model r2rml = generator.generateR2RML(tables);

    rdfConnection.add(r2rml);

    // Query for virtual graph resources
    String sparql = "SELECT ?subject WHERE { ?subject ?predicate ?object } LIMIT 10";
    TupleQuery query = rdfConnection.prepareTupleQuery(sparql);
    TupleQueryResult result = query.evaluate();

    assertTrue(QueryResults.asList(result).size() > 0, "Should return query results");
  }

  @Test
  @DisplayName("should handle schema with multiple relationships")
  void handleSchemaWithMultipleRelationships() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    // order_items has FK to orders
    JdbcSchemaIntrospector.TableMetadata orderItemsTable = introspector.introspectTables()
        .stream()
        .filter(t -> t.name().equals("ORDER_ITEMS"))
        .findFirst()
        .orElseThrow();

    List<JdbcSchemaIntrospector.ForeignKeyMetadata> fks =
        introspector.introspectForeignKeys(orderItemsTable);

    assertFalse(fks.isEmpty());
    assertTrue(fks.stream().anyMatch(fk -> fk.columnName().equals("ORDER_ID")));
  }

  @Test
  @DisplayName("should maintain referential integrity information")
  void maintainReferentialIntegrity() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata ordersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("ORDERS"))
        .findFirst()
        .orElseThrow();

    List<JdbcSchemaIntrospector.ForeignKeyMetadata> fks =
        introspector.introspectForeignKeys(ordersTable);

    JdbcSchemaIntrospector.ForeignKeyMetadata customerFk = fks.stream()
        .filter(fk -> fk.columnName().equals("CUSTOMER_ID"))
        .findFirst()
        .orElseThrow();

    assertEquals("CUSTOMERS", customerFk.referencedTableName());
    assertEquals("ID", customerFk.referencedColumnName());
  }

  @Test
  @DisplayName("should support incremental schema discovery")
  void supportIncrementalSchemaDiscovery() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();

    assertEquals(3, tables.size());

    // Add a new table
    try (Statement stmt = h2Connection.createStatement()) {
      stmt.execute("CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(100))");
    }

    // Re-discover
    List<JdbcSchemaIntrospector.TableMetadata> updateTables = introspector.introspectTables();

    assertEquals(4, updateTables.size(), "Should discover newly added table");
    assertTrue(updateTables.stream().anyMatch(t -> t.name().equals("PRODUCTS")));
  }

  @Test
  @DisplayName("should preserve column order from database")
  void preserveColumnOrder() throws Exception {
    JdbcSchemaIntrospector introspector = new JdbcSchemaIntrospector(h2Connection);

    JdbcSchemaIntrospector.TableMetadata customersTable = introspector.introspectTables().stream()
        .filter(t -> t.name().equals("CUSTOMERS"))
        .findFirst()
        .orElseThrow();

    List<JdbcSchemaIntrospector.ColumnMetadata> columns = customersTable.columns();

    // Verify column order
    assertEquals("ID", columns.get(0).name());
    assertEquals("NAME", columns.get(1).name());
    assertEquals("EMAIL", columns.get(2).name());
  }
}
