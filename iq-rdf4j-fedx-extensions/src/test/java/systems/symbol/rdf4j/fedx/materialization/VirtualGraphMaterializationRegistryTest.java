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
 * Integration tests for VirtualGraphMaterializationRegistry.
 *
 * <p>Tests endpoint registration, discovery, lifecycle management, and system-wide operations.
 */
class VirtualGraphMaterializationRegistryTest {

  private VirtualGraphMaterializationRegistry registry;
  private MockSchemaMaterializerFactory mockSchemaFactory;
  private MockDataMaterializerFactory mockDataFactory;

  @BeforeEach
  void setUp() {
    mockSchemaFactory = new MockSchemaMaterializerFactory();
    mockDataFactory = new MockDataMaterializerFactory();
    registry = new VirtualGraphMaterializationRegistry(mockSchemaFactory, mockDataFactory);
  }

  @Test
  @DisplayName("Registry should register endpoint and return manager")
  void testRegisterEndpoint() throws Exception {
    FedXEndpoint endpoint = createMockEndpoint("test-db", new HashMap<>());

    VirtualGraphMaterializationManager manager = registry.registerEndpoint(endpoint);

    assertNotNull(manager);
    assertEquals(endpoint, manager.getEndpoint());
  }

  @Test
  @DisplayName("Registry should retrieve registered manager by ID")
  void testGetManager() throws Exception {
    FedXEndpoint endpoint = createMockEndpoint("test-db", new HashMap<>());
    registry.registerEndpoint(endpoint);

    VirtualGraphMaterializationManager manager = registry.getManager("test-db");
    assertNotNull(manager);
    assertEquals("test-db", manager.getEndpoint().nodeId());
  }

  @Test
  @DisplayName("Registry should return null for unregistered endpoint")
  void testGetManagerNotFound() {
    VirtualGraphMaterializationManager manager = registry.getManager("non-existent");
    assertNull(manager);
  }

  @Test
  @DisplayName("Registry should list all registered managers")
  void testGetAllManagers() throws Exception {
    FedXEndpoint endpoint1 = createMockEndpoint("db1", new HashMap<>());
    FedXEndpoint endpoint2 = createMockEndpoint("db2", new HashMap<>());

    registry.registerEndpoint(endpoint1);
    registry.registerEndpoint(endpoint2);

    List<VirtualGraphMaterializationManager> managers = registry.getAllManagers();
    assertEquals(2, managers.size());
  }

  @Test
  @DisplayName("Registry should refresh all endpoints")
  void testRefreshAll() throws Exception {
    FedXEndpoint endpoint1 = createMockEndpoint("db1", new HashMap<>());
    FedXEndpoint endpoint2 = createMockEndpoint("db2", new HashMap<>());

    registry.registerEndpoint(endpoint1);
    registry.registerEndpoint(endpoint2);

    registry.refreshAll();

    // Verify both managers were refreshed
    VirtualGraphMaterializationManager m1 = registry.getManager("db1");
    VirtualGraphMaterializationManager m2 = registry.getManager("db2");

    assertEquals(1L, m1.getMetrics().get("refresh.cycles"));
    assertEquals(1L, m2.getMetrics().get("refresh.cycles"));
  }

  @Test
  @DisplayName("Registry should clear all caches")
  void testClearAllCaches() throws Exception {
    FedXEndpoint endpoint1 = createMockEndpoint("db1", new HashMap<>());
    FedXEndpoint endpoint2 = createMockEndpoint("db2", new HashMap<>());

    registry.registerEndpoint(endpoint1);
    registry.registerEndpoint(endpoint2);

    registry.clearAllCaches();

    VirtualGraphMaterializationManager m1 = registry.getManager("db1");
    VirtualGraphMaterializationManager m2 = registry.getManager("db2");

    assertTrue(m1.getMetrics().containsKey("cache.clears"));
    assertTrue(m2.getMetrics().containsKey("cache.clears"));
  }

  @Test
  @DisplayName("Registry should unregister endpoint")
  void testUnregisterEndpoint() throws Exception {
    FedXEndpoint endpoint = createMockEndpoint("test-db", new HashMap<>());
    registry.registerEndpoint(endpoint);

    assertNotNull(registry.getManager("test-db"));

    registry.unregisterEndpoint("test-db");

    assertNull(registry.getManager("test-db"));
  }

  @Test
  @DisplayName("Registry should aggregate system-wide metrics")
  void testGetSystemMetrics() throws Exception {
    FedXEndpoint endpoint1 = createMockEndpoint("db1", new HashMap<>());
    FedXEndpoint endpoint2 = createMockEndpoint("db2", new HashMap<>());

    registry.registerEndpoint(endpoint1);
    registry.registerEndpoint(endpoint2);

    Map<String, Map<String, Object>> systemMetrics = registry.getSystemMetrics();

    assertEquals(2, systemMetrics.size());
    assertTrue(systemMetrics.containsKey("db1"));
    assertTrue(systemMetrics.containsKey("db2"));
  }

  @Test
  @DisplayName("Registry should parse schema materialization policy from endpoint metadata")
  void testParseSchemaMaterializationPolicy() throws Exception {
    Map<String, String> props = new HashMap<>();
    props.put("materialization.schema.strategy", "REALTIME");

    FedXEndpoint endpoint = createMockEndpoint("test-db", props);
    VirtualGraphMaterializationManager manager = registry.registerEndpoint(endpoint);

    Map<String, String> strategies = manager.getStrategies();
    assertTrue(strategies.containsKey("schema"));
  }

  @Test
  @DisplayName("Registry should parse data materialization policy from endpoint metadata")
  void testParseDataMaterializationPolicy() throws Exception {
    Map<String, String> props = new HashMap<>();
    props.put("materialization.data.strategy", "CACHED");

    FedXEndpoint endpoint = createMockEndpoint("test-db", props);
    VirtualGraphMaterializationManager manager = registry.registerEndpoint(endpoint);

    Map<String, String> strategies = manager.getStrategies();
    assertTrue(strategies.containsKey("data"));
  }

  @Test
  @DisplayName("Registry should handle registration of duplicate endpoints gracefully")
  void testDuplicateEndpointRegistration() throws Exception {
    FedXEndpoint endpoint = createMockEndpoint("test-db", new HashMap<>());

    VirtualGraphMaterializationManager m1 = registry.registerEndpoint(endpoint);
    VirtualGraphMaterializationManager m2 = registry.registerEndpoint(endpoint);

    // Should replace the previous manager
    assertEquals(m2, registry.getManager("test-db"));
  }

  // --- Mock implementations ---

  private static class MockSchemaMaterializerFactory implements SchemaMaterializerFactory {
    @Override
    public I_SchemaMaterializer create(SchemaMaterializationPolicy policy) {
      return new MockSchemaMaterializer();
    }

    private static class MockSchemaMaterializer implements I_SchemaMaterializer {
      @Override
      public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint) {
        return new SourceSchema(endpoint.nodeId(), new ArrayList<>(), "MOCK");
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
        return "MOCK";
      }
    }
  }

  private static class MockDataMaterializerFactory implements DataMaterializerFactory {
    @Override
    public I_DataMaterializer create(DataMaterializationPolicy policy) {
      return new MockDataMaterializer();
    }

    private static class MockDataMaterializer implements I_DataMaterializer {
      @Override
      public java.util.List<org.eclipse.rdf4j.query.BindingSet> materialize(
          FedXEndpoint endpoint, String sparqlQuery) {
        return new ArrayList<>();
      }

      @Override
      public void refresh(FedXEndpoint endpoint) {}

      @Override
      public String strategy() {
        return "MOCK";
      }
    }
  }

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
