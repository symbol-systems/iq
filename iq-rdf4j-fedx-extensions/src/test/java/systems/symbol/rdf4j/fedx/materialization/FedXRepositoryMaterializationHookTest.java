package systems.symbol.rdf4j.fedx.materialization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for FedXRepositoryMaterializationHook - the final integration point.
 *
 * <p>Validates that the hook properly connects materialization adapter to FedXRepository
 * lifecycle.
 */
class FedXRepositoryMaterializationHookTest {

  private FedXRepositoryMaterializationHook hook;
  private List<FedXEndpoint> testEndpoints;

  @BeforeEach
  void setUp() {
    hook = new FedXRepositoryMaterializationHook(
        new TestSchemaMaterializerFactory(),
        new TestDataMaterializerFactory());

    testEndpoints = new ArrayList<>();
  }

  @Test
  @DisplayName("Hook should initialize with virtual endpoints")
  void testInitialize() {
    FedXEndpoint ep1 = createTestEndpoint("db1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("db2", "REST");

    hook.initialize(List.of(ep1, ep2));

    assertTrue(hook.isReady());
    assertEquals(2, hook.getManagedEndpointIds().size());
  }

  @Test
  @DisplayName("Hook should throw error when not initialized")
  void testNotInitializedError() {
    assertThrows(MaterializationException.class,
        () -> hook.executeVirtualQuery("db", "SELECT ?s WHERE { ?s ?p ?o }"));
  }

  @Test
  @DisplayName("Hook should execute query on virtual endpoint")
  void testExecuteVirtualQuery() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    List<org.eclipse.rdf4j.query.BindingSet> results =
        hook.executeVirtualQuery("test-db", "SELECT * FROM users");

    assertNotNull(results);
  }

  @Test
  @DisplayName("Hook should retrieve endpoint schema")
  void testGetEndpointSchema() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    SourceSchema schema = hook.getVirtualEndpointSchema("test-db");

    assertNotNull(schema);
    assertEquals("test-db", schema.sourceId());
  }

  @Test
  @DisplayName("Hook should provide endpoint metrics")
  void testGetEndpointMetrics() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    hook.executeVirtualQuery("test-db", "SELECT * FROM t");

    Map<String, Object> metrics = hook.getEndpointMetrics("test-db");

    assertTrue(metrics.containsKey("query.executions"));
  }

  @Test
  @DisplayName("Hook should provide system metrics")
  void testGetSystemMetrics() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("db1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("db2", "REST");

    hook.initialize(List.of(ep1, ep2));

    Map<String, Map<String, Object>> systemMetrics = hook.getSystemMetrics();

    assertEquals(2, systemMetrics.size());
  }

  @Test
  @DisplayName("Hook should refresh all endpoints")
  void testRefresh() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    hook.refresh();

    Map<String, Object> metrics = hook.getEndpointMetrics("test-db");
    assertEquals(1L, metrics.get("refresh.cycles"));
  }

  @Test
  @DisplayName("Hook should clear all caches")
  void testClearCaches() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    hook.clearCaches();

    Map<String, Object> metrics = hook.getEndpointMetrics("test-db");
    assertTrue(metrics.containsKey("cache.clears"));
  }

  @Test
  @DisplayName("Hook should check if managing endpoint")
  void testIsManagingEndpoint() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    assertTrue(hook.isManagingEndpoint("test-db"));
    assertFalse(hook.isManagingEndpoint("unknown"));
  }

  @Test
  @DisplayName("Hook should list managed endpoints")
  void testGetManagedEndpointIds() {
    FedXEndpoint ep1 = createTestEndpoint("db1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("db2", "REST");

    hook.initialize(List.of(ep1, ep2));

    List<String> ids = hook.getManagedEndpointIds();

    assertEquals(2, ids.size());
    assertTrue(ids.contains("db1"));
    assertTrue(ids.contains("db2"));
  }

  @Test
  @DisplayName("Hook should shutdown cleanly")
  void testShutdown() {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    hook.initialize(List.of(endpoint));

    assertTrue(hook.isReady());

    hook.shutdown();

    assertFalse(hook.isReady());
  }

  // --- Test fixtures ---

  private FedXEndpoint createTestEndpoint(String nodeId, String sourceType) {
    Map<String, String> props = new HashMap<>();
    props.put("source.type", sourceType);

    testEndpoints.add(new FedXEndpoint() {
      @Override
      public String nodeId() {
        return nodeId;
      }

      @Override
      public Map<String, String> properties() {
        return props;
      }
    });

    return testEndpoints.get(testEndpoints.size() - 1);
  }

  private static class TestSchemaMaterializerFactory implements SchemaMaterializerFactory {
    @Override
    public I_SchemaMaterializer create(SchemaMaterializationPolicy policy) {
      return new I_SchemaMaterializer() {
        @Override
        public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint) {
          return new SourceSchema(endpoint.nodeId(), new ArrayList<>(), "TEST");
        }

        @Override
        public void refreshSchema(FedXEndpoint endpoint) {}

        @Override
        public boolean isSchemaCached(FedXEndpoint endpoint) {
          return false;
        }

        @Override
        public java.time.Duration getCacheAge(FedXEndpoint endpoint) {
          return null;
        }

        @Override
        public void clearSchemaCache(FedXEndpoint endpoint) {}

        @Override
        public void clearAllSchemaCaches() {}

        @Override
        public String strategy() {
          return "TEST";
        }
      };
    }
  }

  private static class TestDataMaterializerFactory implements DataMaterializerFactory {
    @Override
    public I_DataMaterializer create(DataMaterializationPolicy policy) {
      return new I_DataMaterializer() {
        @Override
        public List<org.eclipse.rdf4j.query.BindingSet> materialize(
            FedXEndpoint endpoint, String sparqlQuery) {
          return new ArrayList<>();
        }

        @Override
        public void refresh(FedXEndpoint endpoint) {}

        @Override
        public String strategy() {
          return "TEST";
        }
      };
    }
  }
}
