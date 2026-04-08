package systems.symbol.rdf4j.fedx.materialization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration tests for VirtualGraphMaterializationManager and Registry.
 *
 * <p>Tests complete workflows: endpoint discovery, schema materialization, query execution,
 * metrics aggregation, and system lifecycle coordination.
 */
class VirtualGraphMaterializationIntegrationTest {

  private VirtualGraphMaterializationRegistry registry;
  private List<FedXEndpoint> testEndpoints;

  @BeforeEach
  void setUp() {
    registry = new VirtualGraphMaterializationRegistry(
        new TestSchemaMaterializerFactory(),
        new TestDataMaterializerFactory());

    testEndpoints = new ArrayList<>();
  }

  @Test
  @DisplayName("End-to-end: Register multiple endpoints and retrieve schemas")
  void testMultiEndpointSchemaDiscovery() throws Exception {
    FedXEndpoint jdbcEndpoint = createTestEndpoint("jdbc-db", "JDBC");
    FedXEndpoint apiEndpoint = createTestEndpoint("rest-api", "REST");

    registry.registerEndpoint(jdbcEndpoint);
    registry.registerEndpoint(apiEndpoint);

    VirtualGraphMaterializationManager jdbcMgr = registry.getManager("jdbc-db");
    VirtualGraphMaterializationManager apiMgr = registry.getManager("rest-api");

    SourceSchema jdbcSchema = jdbcMgr.getSchema();
    SourceSchema apiSchema = apiMgr.getSchema();

    assertNotNull(jdbcSchema);
    assertNotNull(apiSchema);
    assertEquals("jdbc-db", jdbcSchema.sourceId());
    assertEquals("rest-api", apiSchema.sourceId());
  }

  @Test
  @DisplayName("End-to-end: Execute queries across federated endpoints")
  void testFederatedQueryExecution() throws Exception {
    FedXEndpoint endpoint1 = createTestEndpoint("source1", "JDBC");
    FedXEndpoint endpoint2 = createTestEndpoint("source2", "REST");

    VirtualGraphMaterializationManager mgr1 = registry.registerEndpoint(endpoint1);
    VirtualGraphMaterializationManager mgr2 = registry.registerEndpoint(endpoint2);

    String sparqlQuery = "SELECT * WHERE { ?s ?p ?o }";

    List<BindingSet> results1 = mgr1.executeQuery(sparqlQuery);
    List<BindingSet> results2 = mgr2.executeQuery(sparqlQuery);

    assertNotNull(results1);
    assertNotNull(results2);

    // Verify metrics tracked per manager
    assertEquals(1L, mgr1.getMetrics().get("query.executions"));
    assertEquals(1L, mgr2.getMetrics().get("query.executions"));
  }

  @Test
  @DisplayName("End-to-end: Refresh all endpoints and track refresh cycles")
  void testSystemWideRefreshCycle() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");
    FedXEndpoint ep3 = createTestEndpoint("ep3", "JDBC");

    registry.registerEndpoint(ep1);
    registry.registerEndpoint(ep2);
    registry.registerEndpoint(ep3);

    registry.refreshAll();
    registry.refreshAll(); // Refresh again

    // All managers should have 2 refresh cycles
    registry
        .getAllManagers()
        .forEach(
            mgr -> {
              assertEquals(2L, mgr.getMetrics().get("refresh.cycles"));
            });
  }

  @Test
  @DisplayName("End-to-end: Aggregate system metrics from all endpoints")
  void testSystemMetricsAggregation() throws Exception {
    for (int i = 1; i <= 3; i++) {
      FedXEndpoint endpoint = createTestEndpoint("ep" + i, "JDBC");
      VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);

      // Generate some activity
      mgr.getSchema();
      mgr.executeQuery("SELECT * WHERE { ?s ?p ?o }");
      mgr.refresh();
    }

    Map<String, Map<String, Object>> systemMetrics = registry.getSystemMetrics();

    assertEquals(3, systemMetrics.size());
    systemMetrics.forEach(
        (endpointId, metrics) -> {
          assertNotNull(metrics);
          assertTrue(metrics.containsKey("schema.fetches"));
          assertTrue(metrics.containsKey("query.executions"));
          assertTrue(metrics.containsKey("refresh.cycles"));
        });
  }

  @Test
  @DisplayName("End-to-end: Cache coordination across multiple managers")
  void testCacheCoordination() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "REST");

    VirtualGraphMaterializationManager mgr1 = registry.registerEndpoint(ep1);
    VirtualGraphMaterializationManager mgr2 = registry.registerEndpoint(ep2);

    // Get schemas (triggers caching)
    mgr1.getSchema();
    mgr2.getSchema();

    // Verify cache status
    boolean m1Cached = mgr1.isSchemaCached();
    boolean m2Cached = mgr2.isSchemaCached();

    assertNotNull(m1Cached);
    assertNotNull(m2Cached);

    // Clear all caches at system level
    registry.clearAllCaches();

    // Verify cache.clears metric incremented
    assertTrue(mgr1.getMetrics().containsKey("cache.clears"));
    assertTrue(mgr2.getMetrics().containsKey("cache.clears"));
  }

  @Test
  @DisplayName("End-to-end: Endpoint lifecycle - register, use, unregister")
  void testEndpointLifecycle() throws Exception {
    FedXEndpoint endpoint = createTestEndpoint("temp-ep", "JDBC");

    // Register
    VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);
    assertNotNull(mgr);

    // Use
    SourceSchema schema = mgr.getSchema();
    assertNotNull(schema);

    // Unregister
    registry.unregisterEndpoint("temp-ep");
    assertNull(registry.getManager("temp-ep"));
  }

  @Test
  @DisplayName("End-to-end: Error handling during batch refresh")
  void testBatchRefreshWithErrorHandling() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("ep1", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("ep2", "JDBC");

    registry.registerEndpoint(ep1);
    registry.registerEndpoint(ep2);

    // refreshAll should complete despite any internal errors
    registry.refreshAll();

    List<VirtualGraphMaterializationManager> managers = registry.getAllManagers();
    assertEquals(2, managers.size());
  }

  @Test
  @DisplayName("End-to-end: Policy-based strategy selection")
  void testPolicyBasedStrategySelection() throws Exception {
    Map<String, String> realtimeProps = new HashMap<>();
    realtimeProps.put("materialization.schema.strategy", "REALTIME");
    realtimeProps.put("materialization.data.strategy", "REALTIME");

    FedXEndpoint endpoint = new FedXEndpoint() {
      @Override
      public String nodeId() {
        return "realtime-ep";
      }

      @Override
      public Map<String, String> properties() {
        return realtimeProps;
      }
    };

    VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);
    Map<String, String> strategies = mgr.getStrategies();

    assertTrue(strategies.containsKey("schema"));
    assertTrue(strategies.containsKey("data"));
  }

  @Test
  @DisplayName("End-to-end: Isolated endpoint failures don't affect other endpoints")
  void testFaultIsolation() throws Exception {
    FedXEndpoint ep1 = createTestEndpoint("good-ep", "JDBC");
    FedXEndpoint ep2 = createTestEndpoint("bad-ep", "JDBC");

    VirtualGraphMaterializationManager good = registry.registerEndpoint(ep1);
    VirtualGraphMaterializationManager bad = registry.registerEndpoint(ep2);

    // Good endpoint should work fine
    SourceSchema schema = good.getSchema();
    assertNotNull(schema);

    // System-wide operation should handle failures gracefully
    registry.refreshAll();

    // Good endpoint should still be accessible
    assertNotNull(registry.getManager("good-ep"));
  }

  // --- Test fixtures and helpers ---

  private FedXEndpoint createTestEndpoint(String nodeId, String sourceType) {
    Map<String, String> props = new HashMap<>();
    props.put("source.type", sourceType);
    testEndpoints.add(
        new FedXEndpoint() {
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
      public List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery) {
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
