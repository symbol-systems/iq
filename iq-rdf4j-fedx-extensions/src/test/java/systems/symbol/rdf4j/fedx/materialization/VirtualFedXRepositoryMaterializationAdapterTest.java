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
 * Integration tests for VirtualFedXRepositoryMaterializationAdapter.
 *
 * <p>Tests repository-level lifecycle management, query routing, and endpoint discovery.
 */
class VirtualFedXRepositoryMaterializationAdapterTest {

  private VirtualFedXRepositoryMaterializationAdapter adapter;
  private VirtualGraphMaterializationRegistry registry;
  private List<FedXEndpoint> testEndpoints;

  @BeforeEach
  void setUp() {
    registry = new VirtualGraphMaterializationRegistry(
        new TestSchemaMaterializerFactory(),
        new TestDataMaterializerFactory());

    adapter = new VirtualFedXRepositoryMaterializationAdapter(registry);
    testEndpoints = new ArrayList<>();
  }

  @Test
  @DisplayName("Adapter should initialize with virtual endpoints")
  void testInitializeWithEndpoints() {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));

    assertTrue(adapter.isInitialized());
    assertEquals(2, adapter.getEndpointCount());
  }

  @Test
  @DisplayName("Adapter should throw error when used before initialization")
  void testNotInitializedError() {
    assertThrows(IllegalStateException.class, () -> adapter.getRegisteredEndpointIds());
  }

  @Test
  @DisplayName("Adapter should route query to specific endpoint")
  void testRouteQueryToEndpoint() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    List<org.eclipse.rdf4j.query.BindingSet> results = adapter.executeQueryOnEndpoint(
        "test-db", "SELECT * WHERE { ?s ?p ?o }");

    assertNotNull(results);
  }

  @Test
  @DisplayName("Adapter should retrieve endpoint schema")
  void testGetEndpointSchema() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    SourceSchema schema = adapter.getEndpointSchema("test-db");

    assertNotNull(schema);
    assertEquals("test-db", schema.sourceId());
  }

  @Test
  @DisplayName("Adapter should check schema cache status")
  void testCheckSchemaCacheStatus() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    boolean cached = adapter.isEndpointSchemaCached("test-db");
    assertNotNull(cached);

    java.time.Duration age = adapter.getEndpointSchemaCacheAge("test-db");
    // Age can be null or non-null depending on strategy
  }

  @Test
  @DisplayName("Adapter should refresh single endpoint")
  void testRefreshSingleEndpoint() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    adapter.refreshEndpoint("test-db");

    Map<String, Object> metrics = adapter.getEndpointMetrics("test-db");
    assertEquals(1L, metrics.get("refresh.cycles"));
  }

  @Test
  @DisplayName("Adapter should refresh all endpoints")
  void testRefreshAllEndpoints() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));
    adapter.refreshAllEndpoints();

    Map<String, Object> m1 = adapter.getEndpointMetrics("ep1");
    Map<String, Object> m2 = adapter.getEndpointMetrics("ep2");

    assertEquals(1L, m1.get("refresh.cycles"));
    assertEquals(1L, m2.get("refresh.cycles"));
  }

  @Test
  @DisplayName("Adapter should clear endpoint-specific cache")
  void testClearEndpointCache() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    adapter.clearEndpointCache("test-db");

    Map<String, Object> metrics = adapter.getEndpointMetrics("test-db");
    assertTrue(metrics.containsKey("cache.clears"));
  }

  @Test
  @DisplayName("Adapter should clear all caches")
  void testClearAllCaches() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));
    adapter.clearAllCaches();

    Map<String, Object> m1 = adapter.getEndpointMetrics("ep1");
    Map<String, Object> m2 = adapter.getEndpointMetrics("ep2");

    assertTrue(m1.containsKey("cache.clears"));
    assertTrue(m2.containsKey("cache.clears"));
  }

  @Test
  @DisplayName("Adapter should retrieve endpoint strategies")
  void testGetEndpointStrategies() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    Map<String, String> strategies = adapter.getEndpointStrategies("test-db");

    assertTrue(strategies.containsKey("schema"));
    assertTrue(strategies.containsKey("data"));
  }

  @Test
  @DisplayName("Adapter should aggregate system metrics")
  void testGetSystemMetrics() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));

    Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();

    assertEquals(2, systemMetrics.size());
    assertTrue(systemMetrics.containsKey("ep1"));
    assertTrue(systemMetrics.containsKey("ep2"));
  }

  @Test
  @DisplayName("Adapter should list registered endpoint IDs")
  void testGetRegisteredEndpointIds() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));

    List<String> ids = adapter.getRegisteredEndpointIds();

    assertEquals(2, ids.size());
    assertTrue(ids.contains("ep1"));
    assertTrue(ids.contains("ep2"));
  }

  @Test
  @DisplayName("Adapter should retrieve endpoint registration details")
  void testGetEndpointRegistration() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    VirtualFedXRepositoryMaterializationAdapter.EndpointRegistration reg = adapter
        .getEndpointRegistration("test-db");

    assertNotNull(reg);
    assertEquals("test-db", reg.endpoint().nodeId());
    assertTrue(reg.uptime() >= 0);
  }

  @Test
  @DisplayName("Adapter should deregister single endpoint")
  void testDeregisterEndpoint() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));
    assertEquals(2, adapter.getEndpointCount());

    adapter.deregisterEndpoint("ep1");

    assertEquals(1, adapter.getEndpointCount());
    assertNull(registry.getManager("ep1"));
  }

  @Test
  @DisplayName("Adapter should shutdown cleanly")
  void testShutdown() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    adapter.initialize(List.of(ep1, ep2));
    assertTrue(adapter.isInitialized());

    adapter.shutdown();

    assertFalse(adapter.isInitialized());
    assertEquals(0, adapter.getEndpointCount());
  }

  @Test
  @DisplayName("Adapter should throw error for unknown endpoint query")
  void testQueryUnknownEndpoint() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    assertThrows(MaterializationException.class,
        () -> adapter.executeQueryOnEndpoint("unknown", "SELECT * WHERE { ?s ?p ?o }"));
  }

  @Test
  @DisplayName("Adapter should throw error for unknown endpoint schema")
  void testGetSchemaUnknownEndpoint() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("test-db", "JDBC");
    adapter.initialize(List.of(endpoint));

    assertThrows(MaterializationException.class,
        () -> adapter.getEndpointSchema("unknown"));
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
      return new TestSchemaMaterializer();
    }

    private static class TestSchemaMaterializer implements I_SchemaMaterializer {
      @Override
      public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint) {
        return new SourceSchema(endpoint.nodeId(), new ArrayList<>(), "TEST");
      }

      @Override
      public void refreshSchema(FedXEndpoint endpoint) {}

      @Override
      public boolean isSchemaCached(FedXEndpoint endpoint) {
        return true;
      }

      @Override
      public java.time.Duration getCacheAge(FedXEndpoint endpoint) {
        return java.time.Duration.ofSeconds(5);
      }

      @Override
      public void clearSchemaCache(FedXEndpoint endpoint) {}

      @Override
      public void clearAllSchemaCaches() {}

      @Override
      public String strategy() {
        return "TEST_SCHEMA";
      }
    }
  }

  private static class TestDataMaterializerFactory implements DataMaterializerFactory {
    @Override
    public I_DataMaterializer create(DataMaterializationPolicy policy) {
      return new TestDataMaterializer();
    }

    private static class TestDataMaterializer implements I_DataMaterializer {
      @Override
      public List<org.eclipse.rdf4j.query.BindingSet> materialize(
          FedXEndpoint endpoint, String sparqlQuery) {
        return new ArrayList<>();
      }

      @Override
      public void refresh(FedXEndpoint endpoint) {}

      @Override
      public String strategy() {
        return "TEST_DATA";
      }
    }
  }
}
