package systems.symbol.connect.jdbc.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for JDBC schema introspection.
 *
 * Uses H2 in-memory database for fast, isolated tests.
 */
@DisplayName("JdbcSchemaIntrospector")
class JdbcSchemaIntrospectorTest {

  private Connection h2Connection;
  private JdbcSchemaIntrospector introspector;

  @BeforeEach
  void setUp() throws Exception {
    // Set up H2 in-memory database
    h2Connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");

    // Create test schema
    try (Statement stmt = h2Connection.createStatement()) {
      stmt.execute(
          "CREATE TABLE customers ("
              + "  id INT PRIMARY KEY,"
              + "  name VARCHAR(100) NOT NULL,"
              + "  email VARCHAR(100),"
              + "  account_id INT"
              + ")");

      stmt.execute(
          "CREATE TABLE accounts ("
              + "  id INT PRIMARY KEY,"
              + "  account_name VARCHAR(100) NOT NULL,"
              + "  created_date DATE"
              + ")");

      stmt.execute(
          "CREATE TABLE orders ("
              + "  id INT PRIMARY KEY,"
              + "  customer_id INT NOT NULL,"
              + "  amount DECIMAL(10,2),"
              + "  FOREIGN KEY (customer_id) REFERENCES customers(id)"
              + ")");

      // Insert test data
      stmt.execute("INSERT INTO accounts VALUES (1, 'Acme Corp', '2026-01-01')");
      stmt.execute("INSERT INTO accounts VALUES (2, 'Beta Inc', '2026-02-01')");
      stmt.execute("INSERT INTO customers VALUES (1, 'Alice', 'alice@example.com', 1)");
      stmt.execute("INSERT INTO customers VALUES (2, 'Bob', 'bob@example.com', 2)");
      stmt.execute("INSERT INTO orders VALUES (1, 1, 100.50)");
      stmt.execute("INSERT INTO orders VALUES (2, 2, 250.75)");
    }

    introspector = new JdbcSchemaIntrospector(h2Connection);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (h2Connection != null && !h2Connection.isClosed()) {
      h2Connection.close();
    }
  }

  @Test
  @DisplayName("should introspect all tables")
  void introspectAllTables() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();

    assertEquals(3, tables.size(), "Should find 3 tables");
    
    var tableNames = tables.stream()
        .map(JdbcSchemaIntrospector.TableMetadata::getTableName)
        .toList();
    
    assertTrue(tableNames.contains("CUSTOMERS"));
    assertTrue(tableNames.contains("ACCOUNTS"));
    assertTrue(tableNames.contains("ORDERS"));
  }

  @Test
  @DisplayName("should introspect columns for a table")
  void introspectTableColumns() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    var customersTable = tables.stream()
        .filter(t -> "CUSTOMERS".equals(t.getTableName()))
        .findFirst();

    assertTrue(customersTable.isPresent());
    
    var columns = customersTable.get().getColumns();
    assertEquals(4, columns.size());
    
    var columnNames = columns.stream()
        .map(JdbcSchemaIntrospector.ColumnMetadata::getColumnName)
        .toList();
    
    assertTrue(columnNames.contains("ID"));
    assertTrue(columnNames.contains("NAME"));
    assertTrue(columnNames.contains("EMAIL"));
    assertTrue(columnNames.contains("ACCOUNT_ID"));
  }

  @Test
  @DisplayName("should detect primary keys")
  void detectPrimaryKeys() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    var customersTable = tables.stream()
        .filter(t -> "CUSTOMERS".equals(t.getTableName()))
        .findFirst()
        .orElseThrow();

    var pkColumns = customersTable.getColumns().stream()
        .filter(JdbcSchemaIntrospector.ColumnMetadata::isPrimaryKey)
        .toList();

    assertEquals(1, pkColumns.size());
    assertEquals("ID", pkColumns.get(0).getColumnName());
  }

  @Test
  @DisplayName("should detect foreign keys")
  void detectForeignKeys() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    var ordersTable = tables.stream()
        .filter(t -> "ORDERS".equals(t.getTableName()))
        .findFirst()
        .orElseThrow();

    var foreignKeys = ordersTable.getForeignKeys();
    
    assertEquals(1, foreignKeys.size());
    var fk = foreignKeys.get(0);
    assertEquals("CUSTOMER_ID", fk.getFkColumn());
    assertEquals("CUSTOMERS", fk.getReferencedTable());
    assertEquals("ID", fk.getReferencedColumn());
  }

  @Test
  @DisplayName("should get column types")
  void getColumnTypes() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    var customersTable = tables.stream()
        .filter(t -> "CUSTOMERS".equals(t.getTableName()))
        .findFirst()
        .orElseThrow();

    var idColumn = customersTable.getColumns().stream()
        .filter(c -> "ID".equals(c.getColumnName()))
        .findFirst()
        .orElseThrow();

    assertTrue(idColumn.getTypeName().toUpperCase().contains("INT"));

    var emailColumn = customersTable.getColumns().stream()
        .filter(c -> "EMAIL".equals(c.getColumnName()))
        .findFirst()
        .orElseThrow();

    assertTrue(emailColumn.getTypeName().toUpperCase().contains("VARCHAR"));
  }

  @Test
  @DisplayName("should detect nullable columns")
  void detectNullableColumns() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();
    var customersTable = tables.stream()
        .filter(t -> "CUSTOMERS".equals(t.getTableName()))
        .findFirst()
        .orElseThrow();

    var nameColumn = customersTable.getColumns().stream()
        .filter(c -> "NAME".equals(c.getColumnName()))
        .findFirst()
        .orElseThrow();

    assertFalse(nameColumn.isNullable(), "NAME column should be NOT NULL");

    var emailColumn = customersTable.getColumns().stream()
        .filter(c -> "EMAIL".equals(c.getColumnName()))
        .findFirst()
        .orElseThrow();

    assertTrue(emailColumn.isNullable(), "EMAIL column should be nullable");
  }

  @Test
  @DisplayName("should handle multiple schemas independently")
  void multipleTables() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables = introspector.introspectTables();

    var accountsTable = tables.stream()
        .filter(t -> "ACCOUNTS".equals(t.getTableName()))
        .findFirst()
        .orElseThrow();

    assertEquals(3, accountsTable.getColumns().size());
    assertEquals(0, accountsTable.getForeignKeys().size());
  }
}
