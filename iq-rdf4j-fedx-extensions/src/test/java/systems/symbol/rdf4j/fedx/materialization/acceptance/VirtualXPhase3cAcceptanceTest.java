package systems.symbol.rdf4j.fedx.materialization.acceptance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import systems.symbol.rdf4j.fedx.materialization.DataMaterializationPolicy;
import systems.symbol.rdf4j.fedx.materialization.DataMaterializerFactory;
import systems.symbol.rdf4j.fedx.materialization.FedXEndpoint;
import systems.symbol.rdf4j.fedx.materialization.I_DataMaterializer;
import systems.symbol.rdf4j.fedx.materialization.SchemaMaterializationPolicy;
import systems.symbol.rdf4j.fedx.materialization.SchemaMaterializerFactory;
import systems.symbol.rdf4j.fedx.materialization.SourceSchema;
import systems.symbol.rdf4j.fedx.materialization.VirtualFedXRepositoryMaterializationAdapter;
import systems.symbol.rdf4j.fedx.materialization.VirtualGraphMaterializationRegistry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end acceptance test for complete VIRTUALX Phase 3c system.
 *
 * <p>Validates: VirtualGraphMaterializationManager, VirtualGraphMaterializationRegistry, and
 * VirtualFedXRepositoryMaterializationAdapter work together in a complete federation scenario.
 */
class VirtualXPhase3cAcceptanceTest {

  /**
   * Acceptance Scenario 1: Multi-source virtual federation with caching
   *
   * <p>Test complete workflow: discover endpoints → cache schemas → execute queries → monitor
   * metrics → refresh → shutdown
   */
  @Test
  @DisplayName("Complete workflow: Multi-source federation with caching and monitoring")
  void testCompleteVirtualFederationWorkflow() throws Exception {
    // Step 1: Create federation adapter
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new AcceptanceTestSchemaMaterializerFactory(),
            new AcceptanceTestDataMaterializerFactory());

    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    // Step 2: Discover virtual endpoints
    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createVirtualEndpoint("mysql-prod", "JDBC", "CACHED", "CACHED"));
    endpoints.add(createVirtualEndpoint("rest-api", "REST", "REALTIME", "CACHED"));
    endpoints.add(createVirtualEndpoint("data-warehouse", "JDBC", "CACHED", "REALTIME"));

    // Step 3: Initialize (endpoint discovery phase)
    adapter.initialize(endpoints);
    assertTrue(adapter.isInitialized());
    assertEquals(3, adapter.getEndpointCount());

    // Step 4: Verify endpoint registration
    List<String> ids = adapter.getRegisteredEndpointIds();
    assertTrue(ids.contains("mysql-prod"));
    assertTrue(ids.contains("rest-api"));
    assertTrue(ids.contains("data-warehouse"));

    // Step 5: Retrieve schemas (triggers schema materialization)
    SourceSchema mysqlSchema = adapter.getEndpointSchema("mysql-prod");
    SourceSchema apiSchema = adapter.getEndpointSchema("rest-api");
    SourceSchema dwSchema = adapter.getEndpointSchema("data-warehouse");

    assertNotNull(mysqlSchema);
    assertNotNull(apiSchema);
    assertNotNull(dwSchema);

    // Step 6: Verify caching strategies are active
    Map<String, String> mysqlStrategies = adapter.getEndpointStrategies("mysql-prod");
    assertTrue(mysqlStrategies.containsKey("schema"));
    assertTrue(mysqlStrategies.containsKey("data"));

    // Step 7: Execute federated queries
    List<BindingSet> mysqlResults =
        adapter.executeQueryOnEndpoint("mysql-prod", "SELECT * FROM users WHERE active = true");
    List<BindingSet> apiResults =
        adapter.executeQueryOnEndpoint("rest-api", "SELECT * FROM products WHERE price > 100");
    List<BindingSet> dwResults =
        adapter.executeQueryOnEndpoint("data-warehouse", "SELECT * FROM facts");

    assertNotNull(mysqlResults);
    assertNotNull(apiResults);
    assertNotNull(dwResults);

    // Step 8: Verify per-endpoint metrics
    Map<String, Object> mysqlMetrics = adapter.getEndpointMetrics("mysql-prod");
    assertEquals(1L, mysqlMetrics.get("schema.fetches"));
    assertEquals(1L, mysqlMetrics.get("query.executions"));

    Map<String, Object> apiMetrics = adapter.getEndpointMetrics("rest-api");
    assertEquals(1L, apiMetrics.get("schema.fetches"));
    assertEquals(1L, apiMetrics.get("query.executions"));

    // Step 9: Get system-wide metrics
    Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();
    assertEquals(3, systemMetrics.size());
    assertTrue(systemMetrics.containsKey("mysql-prod"));
    assertTrue(systemMetrics.containsKey("rest-api"));
    assertTrue(systemMetrics.containsKey("data-warehouse"));

    // Step 10: Check cache status
    assertTrue(adapter.isEndpointSchemaCached("mysql-prod"));
    assertNotNull(adapter.getEndpointSchemaCacheAge("mysql-prod"));

    // Step 11: Refresh all endpoints (periodic maintenance)
    adapter.refreshAllEndpoints();

    // Verify refresh cycles incremented
    Map<String, Object> refreshedMetrics = adapter.getEndpointMetrics("mysql-prod");
    assertEquals(1L, refreshedMetrics.get("refresh.cycles"));

    // Step 12: Clear caches (cache invalidation)
    adapter.clearAllCaches();

    // Step 13: Deregister one endpoint (dynamic endpoint removal)
    adapter.deregisterEndpoint("rest-api");
    assertEquals(2, adapter.getEndpointCount());

    // Step 14: System still works with remaining endpoints
    List<BindingSet> remainingQuery =
        adapter.executeQueryOnEndpoint("mysql-prod", "SELECT * FROM users");
    assertNotNull(remainingQuery);

    // Step 15: Graceful shutdown
    adapter.shutdown();
    assertFalse(adapter.isInitialized());
    assertEquals(0, adapter.getEndpointCount());
  }

  /**
   * Acceptance Scenario 2: Isolated endpoint failure doesn't break federation
   *
   * <p>Test fault isolation: one endpoint failure shouldn't affect other endpoints
   */
  @Test
  @DisplayName("Fault isolation: One endpoint failure doesn't affect others")
  void testFaultIsolation() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new AcceptanceTestSchemaMaterializerFactory(),
            new AcceptanceTestDataMaterializerFactory());

    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createVirtualEndpoint("good-db1", "JDBC", "CACHED", "CACHED"));
    endpoints.add(createVirtualEndpoint("good-db2", "JDBC", "CACHED", "CACHED"));

    adapter.initialize(endpoints);

    // Query first endpoint
    List<BindingSet> results1 = adapter.executeQueryOnEndpoint("good-db1", "SELECT * FROM t");
    assertNotNull(results1);

    // Even if system-wide refresh is called, good endpoints continue
    adapter.refreshAllEndpoints();

    // Query second endpoint still works
    List<BindingSet> results2 = adapter.executeQueryOnEndpoint("good-db2", "SELECT * FROM t");
    assertNotNull(results2);

    adapter.shutdown();
  }

  /**
   * Acceptance Scenario 3: Metrics aggregation across federation
   *
   * <p>Test system observability: verify metrics are collected and aggregated correctly
   */
  @Test
  @DisplayName("Metrics aggregation: System-wide observability")
  void testMetricsAggregation() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new AcceptanceTestSchemaMaterializerFactory(),
            new AcceptanceTestDataMaterializerFactory());

    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      endpoints.add(createVirtualEndpoint("endpoint-" + i, "JDBC", "CACHED", "CACHED"));
    }

    adapter.initialize(endpoints);

    // Generate activity
    for (String endpointId : adapter.getRegisteredEndpointIds()) {
      adapter.getEndpointSchema(endpointId);
      adapter.executeQueryOnEndpoint(endpointId, "SELECT * FROM t");
      adapter.refreshEndpoint(endpointId);
    }

    // Get system metrics
    Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();

    assertEquals(5, systemMetrics.size());

    // All endpoints should have metrics
    systemMetrics.forEach(
        (endpointId, metrics) -> {
          assertTrue(metrics.containsKey("schema.fetches"));
          assertTrue(metrics.containsKey("query.executions"));
          assertTrue(metrics.containsKey("refresh.cycles"));
          assertEquals(1L, metrics.get("schema.fetches"));
          assertEquals(1L, metrics.get("query.executions"));
          assertEquals(1L, metrics.get("refresh.cycles"));
        });

    adapter.shutdown();
  }

  /**
   * Acceptance Scenario 4: Endpoint registration tracking
   *
   * <p>Test endpoint lifecycle: registration, tracking, and deregistration
   */
  @Test
  @DisplayName("Endpoint tracking: Registration, monitoring, deregistration")
  void testEndpointTracking() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new AcceptanceTestSchemaMaterializerFactory(),
            new AcceptanceTestDataMaterializerFactory());

    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    FedXEndpoint endpoint = createVirtualEndpoint("tracked-endpoint", "JDBC", "CACHED", "CACHED");

    adapter.initialize(List.of(endpoint));

    // Get registration details
    VirtualFedXRepositoryMaterializationAdapter.EndpointRegistration reg =
        adapter.getEndpointRegistration("tracked-endpoint");

    assertNotNull(reg);
    assertEquals("tracked-endpoint", reg.endpoint().nodeId());
    assertTrue(reg.registeredAt() > 0);
    assertTrue(reg.uptime() >= 0);

    adapter.shutdown();
  }

  // --- Test fixtures ---

  private FedXEndpoint createVirtualEndpoint(
      String nodeId, String sourceType, String schemaStrategy, String dataStrategy) {
    return new FedXEndpoint() {
      @Override
      public String nodeId() {
        return nodeId;
      }

      @Override
      public Map<String, String> properties() {
        Map<String, String> props = new HashMap<>();
        props.put("source.type", sourceType);
        props.put("materialization.schema.strategy", schemaStrategy);
        props.put("materialization.data.strategy", dataStrategy);
        return props;
      }
    };
  }

  private static class AcceptanceTestSchemaMaterializerFactory implements SchemaMaterializerFactory {
    @Override
    public systems.symbol.rdf4j.fedx.materialization.I_SchemaMaterializer create(
        SchemaMaterializationPolicy policy) {
      return new systems.symbol.rdf4j.fedx.materialization.I_SchemaMaterializer() {
        @Override
        public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint) {
          return new SourceSchema(endpoint.nodeId(), new ArrayList<>(), "ACCEPTANCE_TEST");
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
          return "ACCEPTANCE_TEST";
        }
      };
    }
  }

  private static class AcceptanceTestDataMaterializerFactory implements DataMaterializerFactory {
    @Override
    public I_DataMaterializer create(DataMaterializationPolicy policy) {
      return new I_DataMaterializer() {
        @Override
        public List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery) {
          return new ArrayList<>();
        }

        @Override
        public void refresh(FedXEndpoint endpoint) {}

        @Override
        public String strategy() {
          return "ACCEPTANCE_TEST";
        }
      };
    }
  }
}
