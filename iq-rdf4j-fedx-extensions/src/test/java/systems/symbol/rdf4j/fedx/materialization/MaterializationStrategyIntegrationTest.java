package systems.symbol.rdf4j.fedx.materialization;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.QueryResultsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for materialization interfaces and implementations.
 *
 * <p>Tests the configuration-driven strategy selection for schema and data materialization. Includes
 * mock endpoint/introspector setup and factory dispatch validation.
 */
class MaterializationStrategyIntegrationTest {

  private MockRemoteSPARQLClient remoteClient;
  private MockSourceIntrospector mockIntrospector;
  private List<I_SourceIntrospector> introspectorList;

  @BeforeEach
  void setUp() {
    remoteClient = new MockRemoteSPARQLClient();
    mockIntrospector = new MockSourceIntrospector();
    introspectorList = new ArrayList<>();
    introspectorList.add(mockIntrospector);
  }

  // ==================== Schema Materialization Tests ====================

  @Test
  @DisplayName("CACHED schema materializer should cache schema and return same instance on hit")
  void testCachedSchemaMaterializerCacheHit() throws Exception {
    Duration ttl = Duration.ofMinutes(1);
    CachedSchemaMaterializer materializer =
        new CachedSchemaMaterializer(introspectorList, ttl);

    FedXEndpoint endpoint = createMockEndpoint("test-db", "JDBC");

    // First call: should introspect (cache miss)
    SourceSchema schema1 = materializer.getOrIntrospectSchema(endpoint);
    assertNotNull(schema1);
    assertEquals(1, mockIntrospector.introspectCallCount);

    // Second call: should return from cache (hit)
    SourceSchema schema2 = materializer.getOrIntrospectSchema(endpoint);
    assertSame(schema1, schema2);
    assertEquals(1, mockIntrospector.introspectCallCount); // No new introspection

    assertTrue(materializer.isSchemaCached(endpoint));
  }

  @Test
  @DisplayName("REALTIME schema materializer should always introspect, never cache")
  void testRealtimeSchemaMaterializerNeverCaches() throws Exception {
    RealtimeSchemaMaterializer materializer =
        new RealtimeSchemaMaterializer(introspectorList);

    FedXEndpoint endpoint = createMockEndpoint("test-db", "JDBC");

    // First call
    SourceSchema schema1 = materializer.getOrIntrospectSchema(endpoint);
    assertNotNull(schema1);
    assertEquals(1, mockIntrospector.introspectCallCount);

    // Second call: should introspect again (no cache)
    SourceSchema schema2 = materializer.getOrIntrospectSchema(endpoint);
    assertNotNull(schema2);
    assertEquals(2, mockIntrospector.introspectCallCount); // Called again

    assertFalse(materializer.isSchemaCached(endpoint));
    assertNull(materializer.getCacheAge(endpoint));
    assertEquals("REALTIME", materializer.strategy());
  }

  @Test
  @DisplayName("Schema cache should expire after TTL and re-introspect")
  void testSchemaCacheExpiry() throws Exception {
    Duration ttl = Duration.ofMillis(100); // Very short for testing
    CachedSchemaMaterializer materializer =
        new CachedSchemaMaterializer(introspectorList, ttl);

    FedXEndpoint endpoint = createMockEndpoint("test-db", "JDBC");

    // First call: cache miss
    SourceSchema schema1 = materializer.getOrIntrospectSchema(endpoint);
    assertEquals(1, mockIntrospector.introspectCallCount);

    // Wait for cache to expire
    Thread.sleep(150);

    // Second call: cache expired, should re-introspect
    SourceSchema schema2 = materializer.getOrIntrospectSchema(endpoint);
    assertEquals(2, mockIntrospector.introspectCallCount);
  }

  @Test
  @DisplayName("Schema materializer factory should create appropriate materializer per policy")
  void testSchemaMaterializerFactory() throws Exception {
    SchemaMaterializerFactory factory =
        new SchemaMaterializerFactory(introspectorList);

    // Test REALTIME
    SchemaMaterializationPolicy realtimePolicy =
        new SchemaMaterializationPolicy(SchemaMaterializationPolicy.Strategy.REALTIME);
    I_SchemaMaterializer realtimeMaterializer = factory.create(realtimePolicy);
    assertInstanceOf(RealtimeSchemaMaterializer.class, realtimeMaterializer);
    assertEquals("REALTIME", realtimeMaterializer.strategy());

    // Test CACHED_1H
    SchemaMaterializationPolicy cachedPolicy =
        new SchemaMaterializationPolicy(SchemaMaterializationPolicy.Strategy.CACHED_1H);
    I_SchemaMaterializer cachedMaterializer = factory.create(cachedPolicy);
    assertInstanceOf(CachedSchemaMaterializer.class, cachedMaterializer);

    // Test default (should be CACHED_1H)
    I_SchemaMaterializer defaultMaterializer = factory.createDefault();
    assertInstanceOf(CachedSchemaMaterializer.class, defaultMaterializer);
  }

  // ==================== Data Materialization Tests ====================

  @Test
  @DisplayName("CACHED data materializer should cache results and return from cache on hit")
  void testCachedDataMaterializerCacheHit() throws Exception {
    Duration ttl = Duration.ofMinutes(1);
    CachedDataMaterializer materializer =
        new CachedDataMaterializer(remoteClient, ttl);

    FedXEndpoint endpoint = createMockEndpoint("api-endpoint", "OpenAPI");
    String query = "SELECT ?name ?email WHERE { ?s ?p ?o }";

    List<BindingSet> results1 = materializer.materialize(endpoint, query);
    assertEquals(1, remoteClient.executeCallCount); // First call

    // Second call with same query: should return from cache
    List<BindingSet> results2 = materializer.materialize(endpoint, query);
    assertEquals(1, remoteClient.executeCallCount); // No new execution

    assertEquals(results1.size(), results2.size());
  }

  @Test
  @DisplayName("WRITE_THROUGH data materializer should always fetch fresh, write to cache")
  void testWriteThroughDataMaterializer() throws Exception {
    Duration ttl = Duration.ofMinutes(1);
    WriteThroughDataMaterializer materializer =
        new WriteThroughDataMaterializer(remoteClient, ttl);

    FedXEndpoint endpoint = createMockEndpoint("api-endpoint", "OpenAPI");
    String query = "SELECT ?name ?email WHERE { ?s ?p ?o }";

    List<BindingSet> results1 = materializer.materialize(endpoint, query);
    assertEquals(1, remoteClient.executeCallCount); // First fetch

    // Second call with same query: should fetch again (always fresh)
    List<BindingSet> results2 = materializer.materialize(endpoint, query);
    assertEquals(2, remoteClient.executeCallCount); // Fetched again

    assertEquals(results1.size(), results2.size());
  }

  @Test
  @DisplayName("Data materializer factory should create appropriate materializer per policy")
  void testDataMaterializerFactory() throws Exception {
    DataMaterializerFactory factory = new DataMaterializerFactory(remoteClient);

    // Test REALTIME
    DataMaterializationPolicy realtimePolicy =
        new DataMaterializationPolicy(DataMaterializationPolicy.Strategy.REALTIME, null);
    I_DataMaterializer realtimeMaterializer = factory.create(realtimePolicy);
    assertInstanceOf(RealtimeDataMaterializer.class, realtimeMaterializer);
    assertEquals("REALTIME", realtimeMaterializer.strategy());

    // Test CACHED_5M
    DataMaterializationPolicy cachedPolicy =
        new DataMaterializationPolicy(DataMaterializationPolicy.Strategy.CACHED_5M, null);
    I_DataMaterializer cachedMaterializer = factory.create(cachedPolicy);
    assertInstanceOf(CachedDataMaterializer.class, cachedMaterializer);

    // Test WRITE_THROUGH
    DataMaterializationPolicy writePolicy =
        new DataMaterializationPolicy(DataMaterializationPolicy.Strategy.WRITE_THROUGH, null);
    I_DataMaterializer writeMaterializer = factory.create(writePolicy);
    assertInstanceOf(WriteThroughDataMaterializer.class, writeMaterializer);

    // Test default (should be REALTIME)
    I_DataMaterializer defaultMaterializer = factory.createDefault();
    assertInstanceOf(RealtimeDataMaterializer.class, defaultMaterializer);
  }

  @Test
  @DisplayName("REALTIME data materializer should always fetch fresh, no caching")
  void testRealtimeDataMaterializer() throws Exception {
    RealtimeDataMaterializer materializer = new RealtimeDataMaterializer(remoteClient);

    FedXEndpoint endpoint = createMockEndpoint("api-endpoint", "OpenAPI");
    String query = "SELECT ?name ?email WHERE { ?s ?p ?o }";

    List<BindingSet> results1 = materializer.materialize(endpoint, query);
    assertEquals(1, remoteClient.executeCallCount);

    // Second call: should fetch again (no cache)
    List<BindingSet> results2 = materializer.materialize(endpoint, query);
    assertEquals(2, remoteClient.executeCallCount);

    assertEquals("REALTIME", materializer.strategy());
  }

  // ==================== Policy Configuration Tests ====================

  @Test
  @DisplayName("Schema policy should parse from endpoint metadata")
  void testSchemaPolicyFromMetadata() {
    Map<String, String> props = new HashMap<>();
    props.put("schema.strategy", "cached_24h");
    props.put("schema.ttl.minutes", "1440");

    SchemaMaterializationPolicy policy = SchemaMaterializationPolicy.fromMetadata(props);

    assertEquals(SchemaMaterializationPolicy.Strategy.CACHED_24H, policy.strategy());
    assertEquals(Duration.ofMinutes(1440), policy.ttl());
  }

  @Test
  @DisplayName("Data policy should parse from endpoint metadata")
  void testDataPolicyFromMetadata() {
    Map<String, String> props = new HashMap<>();
    props.put("data.strategy", "cached_1h");
    props.put("data.ttl.minutes", "60");

    DataMaterializationPolicy policy = DataMaterializationPolicy.fromMetadata(props);

    assertEquals(DataMaterializationPolicy.Strategy.CACHED_1H, policy.strategy());
    assertEquals(Duration.ofMinutes(60), policy.ttl());
  }

  @Test
  @DisplayName("Default policies should apply when metadata missing")
  void testDefaultPolicies() {
    Map<String, String> emptyProps = new HashMap<>();

    SchemaMaterializationPolicy schemaPolicy = SchemaMaterializationPolicy.fromMetadata(emptyProps);
    assertEquals(SchemaMaterializationPolicy.Strategy.CACHED_1H, schemaPolicy.strategy()); // Default

    DataMaterializationPolicy dataPolicy = DataMaterializationPolicy.fromMetadata(emptyProps);
    assertEquals(DataMaterializationPolicy.Strategy.REALTIME, dataPolicy.strategy()); // Default
  }

  // ==================== Source Introspector Dispatch Tests ====================

  @Test
  @DisplayName("Cached schema materializer should use router to select correct introspector")
  void testSchemaMaterializerRouting() throws Exception {
    MockJdbcIntrospector jdbcIntrospector = new MockJdbcIntrospector();
    MockOpenApiIntrospector openApiIntrospector = new MockOpenApiIntrospector();

    List<I_SourceIntrospector> introspectors = new ArrayList<>();
    introspectors.add(jdbcIntrospector);
    introspectors.add(openApiIntrospector);

    CachedSchemaMaterializer materializer =
        new CachedSchemaMaterializer(introspectors, Duration.ofMinutes(1));

    // JDBC endpoint should use JDBC introspector
    FedXEndpoint jdbcEndpoint = createMockEndpoint("db1", "JDBC");
    materializer.getOrIntrospectSchema(jdbcEndpoint);
    assertTrue(jdbcIntrospector.wasCalled);
    assertFalse(openApiIntrospector.wasCalled);

    // Reset
    jdbcIntrospector.wasCalled = false;
    openApiIntrospector.wasCalled = false;

    // OpenAPI endpoint should use OpenAPI introspector
    FedXEndpoint apiEndpoint = createMockEndpoint("api1", "OpenAPI");
    materializer.getOrIntrospectSchema(apiEndpoint);
    assertFalse(jdbcIntrospector.wasCalled);
    assertTrue(openApiIntrospector.wasCalled);
  }

  // ==================== Helper Classes ====================

  private FedXEndpoint createMockEndpoint(String nodeId, String sourceType) {
    Map<String, String> props = new HashMap<>();
    props.put("source.type", sourceType);
    if ("OpenAPI".equals(sourceType)) {
      props.put("openapi.spec.url", "https://example.com/openapi.json");
    }
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

  private static class MockRemoteSPARQLClient implements I_RemoteSPARQLClient {
    int executeCallCount = 0;

    @Override
    public List<BindingSet> execute(FedXEndpoint endpoint, String sparqlQuery) {
      executeCallCount++;
      return new ArrayList<>(); // Empty results
    }
  }

  private static class MockSourceIntrospector implements I_SourceIntrospector {
    int introspectCallCount = 0;

    @Override
    public SourceSchema introspectSchema(FedXEndpoint endpoint) {
      introspectCallCount++;
      return new SourceSchema(
          endpoint.nodeId(),
          new ArrayList<>(),
          System.currentTimeMillis(),
          "MOCK");
    }

    @Override
    public boolean canHandle(FedXEndpoint endpoint) {
      return true; // Accept all
    }

    @Override
    public String sourceType() {
      return "MOCK";
    }
  }

  private static class MockJdbcIntrospector implements I_SourceIntrospector {
    boolean wasCalled = false;

    @Override
    public SourceSchema introspectSchema(FedXEndpoint endpoint) {
      wasCalled = true;
      return new SourceSchema(endpoint.nodeId(), new ArrayList<>(),
          System.currentTimeMillis(), "JDBC");
    }

    @Override
    public boolean canHandle(FedXEndpoint endpoint) {
      return "JDBC".equalsIgnoreCase(endpoint.properties().get("source.type"));
    }

    @Override
    public String sourceType() {
      return "JDBC";
    }
  }

  private static class MockOpenApiIntrospector implements I_SourceIntrospector {
    boolean wasCalled = false;

    @Override
    public SourceSchema introspectSchema(FedXEndpoint endpoint) {
      wasCalled = true;
      return new SourceSchema(endpoint.nodeId(), new ArrayList<>(),
          System.currentTimeMillis(), "OpenAPI");
    }

    @Override
    public boolean canHandle(FedXEndpoint endpoint) {
      return "OpenAPI".equalsIgnoreCase(endpoint.properties().get("source.type"));
    }

    @Override
    public String sourceType() {
      return "OpenAPI";
    }
  }
}
