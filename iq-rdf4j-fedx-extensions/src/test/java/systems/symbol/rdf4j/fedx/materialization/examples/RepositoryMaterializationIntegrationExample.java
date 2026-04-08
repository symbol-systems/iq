package systems.symbol.rdf4j.fedx.materialization.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;

import systems.symbol.rdf4j.fedx.materialization.DataMaterializationPolicy;
import systems.symbol.rdf4j.fedx.materialization.DataMaterializerFactory;
import systems.symbol.rdf4j.fedx.materialization.FedXEndpoint;
import systems.symbol.rdf4j.fedx.materialization.I_DataMaterializer;
import systems.symbol.rdf4j.fedx.materialization.SchemaMaterializationPolicy;
import systems.symbol.rdf4j.fedx.materialization.SchemaMaterializerFactory;
import systems.symbol.rdf4j.fedx.materialization.SourceSchema;
import systems.symbol.rdf4j.fedx.materialization.VirtualFedXRepositoryMaterializationAdapter;
import systems.symbol.rdf4j.fedx.materialization.VirtualGraphMaterializationRegistry;

/**
 * Example: Integrating VirtualFedXRepositoryMaterializationAdapter into a federation repository.
 *
 * <p>This example demonstrates:
 * 1. Creating materialization factories
 * 2. Initializing the adapter
 * 3. Discovering virtual endpoints
 * 4. Routing queries through materialization
 * 5. Managing caches and refresh cycles
 * 6. Monitoring system metrics
 */
public class RepositoryMaterializationIntegrationExample {

  /**
   * Example 1: Basic initialization with virtual endpoints
   */
  public static void example1_BasicInitialization() {
    // Step 1: Create factories (in production, these would be dependency-injected)
    SchemaMaterializerFactory schemaFactory = new ExampleSchemaMaterializerFactory();
    DataMaterializerFactory dataFactory = new ExampleDataMaterializerFactory();

    // Step 2: Create registry
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(schemaFactory, dataFactory);

    // Step 3: Create adapter
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    // Step 4: Create virtual endpoints with materialization policies
    List<FedXEndpoint> virtualEndpoints = new ArrayList<>();
    virtualEndpoints.add(createJdbcEndpoint("database-1", "JDBC",
        "REALTIME", // schema strategy
        "CACHED"    // data strategy
    ));
    virtualEndpoints.add(createRestEndpoint("rest-api-1", "REST",
        "CACHED",   // schema strategy
        "REALTIME"  // data strategy
    ));

    // Step 5: Initialize adapter (typically during repository startup)
    adapter.initialize(virtualEndpoints);

    System.out.println("✓ Adapter initialized with " + adapter.getEndpointCount()
        + " virtual endpoints");
  }

  /**
   * Example 2: Querying through materialization system
   */
  public static void example2_QueryRouting() throws Exception {
    // Setup
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "REALTIME", "CACHED"));
    adapter.initialize(endpoints);

    // Step 1: Route query to specific endpoint
    String sparqlQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";
    List<BindingSet> results = adapter.executeQueryOnEndpoint("database-1", sparqlQuery);

    System.out.println("✓ Query executed, returned " + results.size() + " bindings");

    // Step 2: Check metrics
    Map<String, Object> metrics = adapter.getEndpointMetrics("database-1");
    System.out.println("✓ Query executions: " + metrics.get("query.executions"));
    System.out.println("✓ Query latency: " + metrics.get("query.latency.ms") + "ms");
  }

  /**
   * Example 3: Schema discovery and caching
   */
  public static void example3_SchemaDiscovery() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "CACHED", "REALTIME"));
    adapter.initialize(endpoints);

    // Step 1: Retrieve schema
    SourceSchema schema = adapter.getEndpointSchema("database-1");
    System.out.println("✓ Schema retrieved for source: " + schema.sourceId());

    // Step 2: Check cache status
    boolean isCached = adapter.isEndpointSchemaCached("database-1");
    System.out.println("✓ Schema is cached: " + isCached);

    if (isCached) {
      java.time.Duration cacheAge = adapter.getEndpointSchemaCacheAge("database-1");
      System.out.println("✓ Cache age: " + cacheAge.getSeconds() + " seconds");
    }

    // Step 3: Get active strategies
    Map<String, String> strategies = adapter.getEndpointStrategies("database-1");
    System.out.println("✓ Schema strategy: " + strategies.get("schema"));
    System.out.println("✓ Data strategy: " + strategies.get("data"));
  }

  /**
   * Example 4: Refresh management
   */
  public static void example4_RefreshManagement() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "CACHED", "CACHED"));
    endpoints.add(createRestEndpoint("rest-api-1", "REST", "CACHED", "CACHED"));
    adapter.initialize(endpoints);

    // Step 1: Refresh single endpoint
    adapter.refreshEndpoint("database-1");
    System.out.println("✓ Cached schema for database-1 refreshed");

    // Step 2: Refresh all endpoints
    adapter.refreshAllEndpoints();
    System.out.println("✓ All cached schemas refreshed");

    // Step 3: Check refresh metrics
    Map<String, Object> metrics = adapter.getEndpointMetrics("database-1");
    System.out.println("✓ Refresh cycles: " + metrics.get("refresh.cycles"));
  }

  /**
   * Example 5: System metrics and monitoring
   */
  public static void example5_SystemMetrics() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "REALTIME", "CACHED"));
    endpoints.add(createRestEndpoint("rest-api-1", "REST", "CACHED", "REALTIME"));
    endpoints.add(createJdbcEndpoint("database-2", "JDBC", "CACHED", "CACHED"));
    adapter.initialize(endpoints);

    // Execute some operations to generate metrics
    adapter.executeQueryOnEndpoint("database-1", "SELECT * WHERE { ?s ?p ?o }");
    adapter.getEndpointSchema("rest-api-1");
    adapter.refreshEndpoint("database-2");

    // Get system-wide metrics
    Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();

    System.out.println("✓ System Metrics for " + systemMetrics.size() + " endpoints:");
    systemMetrics.forEach((endpointId, metrics) -> {
      System.out.println("  [" + endpointId + "]");
      System.out.println("    - Schema fetches: " + metrics.get("schema.fetches"));
      System.out.println("    - Query executions: " + metrics.get("query.executions"));
      System.out.println("    - Refresh cycles: " + metrics.get("refresh.cycles"));
    });
  }

  /**
   * Example 6: Lifecycle management (startup/shutdown)
   */
  public static void example6_LifecycleManagement() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    // Startup: Initialize with discovered endpoints
    List<FedXEndpoint> discoveredEndpoints = new ArrayList<>();
    discoveredEndpoints.add(createJdbcEndpoint("database-1", "JDBC", "CACHED", "CACHED"));
    discoveredEndpoints.add(createRestEndpoint("rest-api-1", "REST", "REALTIME", "CACHED"));

    adapter.initialize(discoveredEndpoints);
    System.out.println("✓ Repository materialization initialized");

    // Runtime: Use adapter for queries
    adapter.executeQueryOnEndpoint("database-1", "SELECT * WHERE { ?s ?p ?o }");

    // Periodic refresh: Part of repository's refresh cycle
    adapter.refreshAllEndpoints();

    // Shutdown: Clean deregistration and cache clearing
    adapter.shutdown();
    System.out.println("✓ Repository materialization shut down");
  }

  /**
   * Example 7: Cache management
   */
  public static void example7_CacheManagement() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "CACHED", "CACHED"));
    adapter.initialize(endpoints);

    // Get schema (triggers caching)
    SourceSchema schema = adapter.getEndpointSchema("database-1");
    System.out.println("✓ Schema retrieved and cached");

    // Check cache status
    boolean isCached = adapter.isEndpointSchemaCached("database-1");
    System.out.println("✓ Schema cached: " + isCached);

    // Clear single endpoint cache
    adapter.clearEndpointCache("database-1");
    System.out.println("✓ Cache cleared for database-1");

    // Clear all caches (system-wide)
    adapter.clearAllCaches();
    System.out.println("✓ All caches cleared");
  }

  /**
   * Example 8: Endpoint registration tracking
   */
  public static void example8_EndpointTracking() throws Exception {
    VirtualGraphMaterializationRegistry registry =
        new VirtualGraphMaterializationRegistry(
            new ExampleSchemaMaterializerFactory(),
            new ExampleDataMaterializerFactory());
    VirtualFedXRepositoryMaterializationAdapter adapter =
        new VirtualFedXRepositoryMaterializationAdapter(registry);

    List<FedXEndpoint> endpoints = new ArrayList<>();
    endpoints.add(createJdbcEndpoint("database-1", "JDBC", "REALTIME", "CACHED"));
    endpoints.add(createRestEndpoint("rest-api-1", "REST", "CACHED", "REALTIME"));
    adapter.initialize(endpoints);

    // List all registered endpoints
    List<String> endpointIds = adapter.getRegisteredEndpointIds();
    System.out.println("✓ Registered endpoints: " + String.join(", ", endpointIds));

    // Get endpoint registration details
    VirtualFedXRepositoryMaterializationAdapter.EndpointRegistration reg =
        adapter.getEndpointRegistration("database-1");
    System.out.println("✓ Endpoint: " + reg.endpoint().nodeId());
    System.out.println("✓ Registered at: " + reg.registeredAt());
    System.out.println("✓ Uptime: " + reg.uptime() + "ms");

    // Deregister an endpoint
    adapter.deregisterEndpoint("rest-api-1");
    System.out.println("✓ Deregistered rest-api-1");
    System.out.println("✓ Remaining endpoints: " + adapter.getEndpointCount());
  }

  // --- Test fixture helpers ---

  private static FedXEndpoint createJdbcEndpoint(
      String nodeId,
      String sourceType,
      String schemaStrategy,
      String dataStrategy) {
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
        props.put("jdbc.url", "jdbc:mysql://localhost:3306/db");
        props.put("jdbc.driver", "com.mysql.jdbc.Driver");
        return props;
      }
    };
  }

  private static FedXEndpoint createRestEndpoint(
      String nodeId,
      String sourceType,
      String schemaStrategy,
      String dataStrategy) {
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
        props.put("rest.base.url", "https://api.example.com/v1");
        props.put("rest.auth.type", "bearer");
        return props;
      }
    };
  }

  // --- Example implementations (would be replaced with real ones) ---

  private static class ExampleSchemaMaterializerFactory implements SchemaMaterializerFactory {
    @Override
    public systems.symbol.rdf4j.fedx.materialization.I_SchemaMaterializer create(
        SchemaMaterializationPolicy policy) {
      return new systems.symbol.rdf4j.fedx.materialization.I_SchemaMaterializer() {
        @Override
        public SourceSchema getOrIntrospectSchema(FedXEndpoint endpoint) {
          return new SourceSchema(endpoint.nodeId(), new ArrayList<>(), "EXAMPLE");
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
          return "EXAMPLE";
        }
      };
    }
  }

  private static class ExampleDataMaterializerFactory implements DataMaterializerFactory {
    @Override
    public I_DataMaterializer create(
        DataMaterializationPolicy policy) {
      return new I_DataMaterializer() {
        @Override
        public List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery) {
          return new ArrayList<>();
        }

        @Override
        public void refresh(FedXEndpoint endpoint) {}

        @Override
        public String strategy() {
          return "EXAMPLE";
        }
      };
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println("=== VIRTUALX Repository Materialization Integration Examples ===\n");

    System.out.println("Example 1: Basic Initialization");
    example1_BasicInitialization();
    System.out.println();

    System.out.println("Example 2: Query Routing");
    example2_QueryRouting();
    System.out.println();

    System.out.println("Example 3: Schema Discovery");
    example3_SchemaDiscovery();
    System.out.println();

    System.out.println("Example 4: Refresh Management");
    example4_RefreshManagement();
    System.out.println();

    System.out.println("Example 5: System Metrics");
    example5_SystemMetrics();
    System.out.println();

    System.out.println("Example 6: Lifecycle Management");
    example6_LifecycleManagement();
    System.out.println();

    System.out.println("Example 7: Cache Management");
    example7_CacheManagement();
    System.out.println();

    System.out.println("Example 8: Endpoint Tracking");
    example8_EndpointTracking();
    System.out.println();

    System.out.println("=== All Examples Completed ===");
  }
}
