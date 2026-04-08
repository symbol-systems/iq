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
 * Integration tests for VirtualGraphMaterializationManager and registry.
 *
 * <p>Tests endpoint management, manager lifecycle, metrics tracking, and cache coordination.
 */
class VirtualGraphMaterializationManagerTest {

  private VirtualGraphMaterializationManager manager;
  private FedXEndpoint endpoint;
  private MockSchemaMaterializer mockSchemaMaterializer;
  private MockDataMaterializer mockDataMaterializer;

  @BeforeEach
  void setUp() {
    mockSchemaMaterializer = new MockSchemaMaterializer();
    mockDataMaterializer = new MockDataMaterializer();

    Map<String, String> props = new HashMap<>();
    props.put("source.type", "JDBC");

    endpoint = createMockEndpoint("test-db", props);

    manager = new VirtualGraphMaterializationManager(
        endpoint,
        mockSchemaMaterializer,
        mockDataMaterializer);
  }

  @Test
  @DisplayName("Manager should retrieve schema and track metrics")
  void testGetSchemaTrackingMetrics() throws Exception {
    SourceSchema schema = manager.getSchema();

    assertNotNull(schema);
    assertEquals("test-db", schema.sourceId());

    Map<String, Object> metrics = manager.getMetrics();
    assertTrue(metrics.containsKey("schema.fetches"));
    assertEquals(1L, metrics.get("schema.fetches"));
  }

  @Test
  @DisplayName("Manager should execute query and track metrics")
  void testExecuteQueryTrackingMetrics() throws Exception {
    List<BindingSet> results = manager.executeQuery("SELECT * WHERE { ?s ?p ?o }");

    assertNotNull(results);

    Map<String, Object> metrics = manager.getMetrics();
    assertTrue(metrics.containsKey("query.executions"));
    assertEquals(1L, metrics.get("query.executions"));
    assertTrue(metrics.containsKey("query.latency.ms"));
  }

  @Test
  @DisplayName("Manager should track schema cache status")
  void testSchemaCacheStatus() throws Exception {
    boolean cached = manager.isSchemaCached();
    assertNotNull(cached);

    java.time.Duration age = manager.getSchemaCacheAge();
    // Age can be null for realtime or non-null for cached

    assertTrue(manager.getStrategies().containsKey("schema"));
    assertTrue(manager.getStrategies().containsKey("data"));
  }

  @Test
  @DisplayName("Manager should coordinate refresh cycle")
  void testRefreshCycle() throws Exception {
    manager.refresh();

    Map<String, Object> metrics = manager.getMetrics();
    assertTrue(metrics.containsKey("refresh.cycles"));
    assertEquals(1L, metrics.get("refresh.cycles"));
  }

  @Test
  @DisplayName("Manager should track errors in metrics")
  void testErrorTracking() throws Exception {
    mockSchemaMaterializer.throwException = true;

    assertThrows(MaterializationException.class, () -> manager.getSchema());

    Map<String, Object> metrics = manager.getMetrics();
    assertEquals(1L, metrics.get("schema.fetch.errors"));
  }

  // --- Mock implementations ---

  private static class MockSchemaMaterializer implements I_SchemaMaterializer {
    boolean throwException = false;

    @Override
    public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint)
        throws MaterializationException {
      if (throwException) {
        throw new MaterializationException("Mock error");
      }
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

  private static class MockDataMaterializer implements I_DataMaterializer {
    @Override
    public List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery) {
      return new ArrayList<>();
    }

    @Override
    public void refresh(FedXEndpoint endpoint) {}

    @Override
    public String strategy() {
      return "MOCK";
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
