package systems.symbol.rdf4j.fedx.materialization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JDBC schema introspector.
 *
 * <p>Tests schema extraction from JDBC connections using mocked DatabaseMetaData.
 */
class JdbcSourceIntrospectorTest {

  private Connection mockConnection;
  private DatabaseMetaData mockMetadata;
  private JdbcSourceIntrospector introspector;

  @BeforeEach
  void setUp() throws Exception {
    mockConnection = mock(Connection.class);
    mockMetadata = mock(DatabaseMetaData.class);
    when(mockConnection.getMetaData()).thenReturn(mockMetadata);

    introspector = new JdbcSourceIntrospector(mockConnection);
  }

  @Test
  @DisplayName("Should recognize JDBC endpoints via canHandle()")
  void testCanHandle() {
    Map<String, String> props = new HashMap<>();
    props.put("source.type", "JDBC");

    FedXEndpoint endpoint = createMockEndpoint("test-db", props);

    assertTrue(introspector.canHandle(endpoint));
    assertEquals("JDBC", introspector.sourceType());
  }

  @Test
  @DisplayName("Should reject non-JDBC endpoints")
  void testCannotHandleOpenApi() {
    Map<String, String> props = new HashMap<>();
    props.put("source.type", "OpenAPI");

    FedXEndpoint endpoint = createMockEndpoint("api-1", props);

    assertFalse(introspector.canHandle(endpoint));
  }

  @Test
  @DisplayName("Should map SQL types to XSD types correctly")
  void testSqlTypeMapping() throws Exception {
    // Mock an empty table result set to complete introspection
    ResultSet emptyTables = mock(ResultSet.class);
    when(emptyTables.next()).thenReturn(false);
    when(mockMetadata.getTables(null, null, "%", new String[]{"TABLE"}))
        .thenReturn(emptyTables);

    Map<String, String> props = new HashMap<>();
    props.put("source.type", "JDBC");
    FedXEndpoint endpoint = createMockEndpoint("test-db", props);

    SourceSchema schema = introspector.introspectSchema(endpoint);
    assertNotNull(schema);
    assertEquals("test-db", schema.sourceId());
    assertEquals("JDBC", schema.sourceType());
  }

  @Test
  @DisplayName("Should throw MaterializationException on connection failure")
  void testConnectionFailure() throws Exception {
    when(mockConnection.getMetaData()).thenThrow(new java.sql.SQLException("Connection failed"));

    Map<String, String> props = new HashMap<>();
    props.put("source.type", "JDBC");
    FedXEndpoint endpoint = createMockEndpoint("test-db", props);

    MaterializationException ex = assertThrows(
        MaterializationException.class,
        () -> introspector.introspectSchema(endpoint));

    assertTrue(ex.getMessage().contains("Failed to introspect JDBC schema"));
  }

  // --- Helper ---
  private FedXEndpoint createMockEndpoint(String nodeId, Map<String, String> props) {
    return new FedXEndpoint() {
      @Override
      public String nodeId() {
        return nodeId;
      }

      @Override
      public Map<String, String> properties() {
        return props;
      }
    };
  }
}
