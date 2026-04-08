# VIRTUALX Phase 3 Developer Guide

**Complete Guide to Virtual Graph Materialization System**

---

## Overview

VIRTUALX Phase 3 implements a complete materialization framework for virtual federation graphs, enabling automatic schema discovery, configurable data fetching strategies, and system-wide lifecycle management.

**Three-Layer Architecture:**
1. **Managers** - Per-endpoint coordination (schema + data materialization)
2. **Registry** - System-wide discovery and lifecycle
3. **Adapter** - Repository integration layer

---

## Layer 1: Managers (VirtualGraphMaterializationManager)

### Purpose
Coordinates schema discovery and query execution for a single virtual endpoint.

### Key Operations

```java
SourceSchema schema = manager.getSchema();           // Get schema (realtime or cached)
List<BindingSet> results = manager.executeQuery(sql); // Execute query
manager.refresh();                                   // Refresh both caches
boolean cached = manager.isSchemaCached();           // Check cache status
Duration age = manager.getSchemaCacheAge();          // Get cache age
manager.clearCaches();                               // Full eviction
Map<String, String> strats = manager.getStrategies(); // Active strategies
Map<String, Object> metrics = manager.getMetrics();   // Performance metrics
```

### Metrics Tracked

Per-endpoint metrics automatically collected:
- **Schema Operations**: fetches, latency (ms), errors
- **Query Operations**: executions, latency (ms), errors
- **Lifecycle**: refresh cycles, cache clears

### Example: Creating a Manual Manager

```java
// Create materializers (or inject from DI container)
I_SchemaMaterializer schema = new RealtimeSchemaMaterializer();
I_DataMaterializer data = new CachedDataMaterializer(cacheConfig);

// Create manager for single endpoint
FedXEndpoint endpoint = ...;
VirtualGraphMaterializationManager manager = 
    new VirtualGraphMaterializationManager(endpoint, schema, data);

// Use manager
SourceSchema discovered = manager.getSchema();
List<BindingSet> rows = manager.executeQuery("SELECT * WHERE { ?s ?p ?o }");
```

---

## Layer 2: Registry (VirtualGraphMaterializationRegistry)

### Purpose
System-wide endpoint discovery, manager lifecycle, and aggregated metrics.

### Key Operations

```java
// Register endpoint (discovers policies, creates manager)
VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);

// Retrieve managers
VirtualGraphMaterializationManager mgr = registry.getManager("endpoint-id");
List<VirtualGraphMaterializationManager> all = registry.getAllManagers();

// System operations
registry.refreshAll();                          // Refresh all endpoints
registry.clearAllCaches();                      // Clear all caches
Map<String, Map<String, Object>> metrics = registry.getSystemMetrics();

// Deregistration
registry.unregisterEndpoint("endpoint-id");
```

### Policy Discovery

Registry automatically parses endpoint metadata:

```java
// In endpoint.properties():
materialization.schema.strategy = REALTIME|CACHED|WRITE_THROUGH
materialization.data.strategy   = REALTIME|CACHED|WRITE_THROUGH

// Registry creates appropriate materializers via factories
```

### Example: Creating and Using Registry

```java
SchemaMaterializerFactory schemaFactory = new DefaultSchemaMaterializerFactory();
DataMaterializerFactory dataFactory = new DefaultDataMaterializerFactory();

VirtualGraphMaterializationRegistry registry = 
    new VirtualGraphMaterializationRegistry(schemaFactory, dataFactory);

// Auto-discovery: Registry parses policies from endpoint metadata
VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);

// System-wide operations
registry.refreshAll();
Map<String, Map<String, Object>> systemMetrics = registry.getSystemMetrics();
```

---

## Layer 3: Adapter (VirtualFedXRepositoryMaterializationAdapter)

### Purpose
Repository integration layer that provides lifecycle management and high-level API for query routing.

### Key Operations

```java
// Lifecycle
adapter.initialize(List<FedXEndpoint> virtualEndpoints);  // Startup
adapter.shutdown();                                       // Shutdown

// Query execution
List<BindingSet> results = 
    adapter.executeQueryOnEndpoint("endpoint-id", sparqlQuery);

// Schema management
SourceSchema schema = adapter.getEndpointSchema("endpoint-id");
boolean cached = adapter.isEndpointSchemaCached("endpoint-id");
Duration age = adapter.getEndpointSchemaCacheAge("endpoint-id");

// Refresh control
adapter.refreshEndpoint("endpoint-id");  // Single endpoint
adapter.refreshAllEndpoints();           // All endpoints

// Cache management
adapter.clearEndpointCache("endpoint-id");  // Single endpoint
adapter.clearAllCaches();                   // All endpoints

// Monitoring
Map<String, Object> metrics = adapter.getEndpointMetrics("endpoint-id");
Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();
List<String> ids = adapter.getRegisteredEndpointIds();

// Endpoint management
adapter.deregisterEndpoint("endpoint-id");
int count = adapter.getEndpointCount();

// Endpoint tracking
VirtualFedXRepositoryMaterializationAdapter.EndpointRegistration reg = 
    adapter.getEndpointRegistration("endpoint-id");
long uptime = reg.uptime();  // ms since registration
```

### Example: Full Repository Integration

```java
public class MyVirtualRepository {
    private VirtualFedXRepositoryMaterializationAdapter adapter;
    
    public MyVirtualRepository(List<FedXEndpoint> virtualEndpoints) {
        VirtualGraphMaterializationRegistry registry = 
            new VirtualGraphMaterializationRegistry(
                new DefaultSchemaMaterializerFactory(),
                new DefaultDataMaterializerFactory());
        
        adapter = new VirtualFedXRepositoryMaterializationAdapter(registry);
        adapter.initialize(virtualEndpoints);
    }
    
    public List<BindingSet> query(String endpointId, String sparql) {
        return adapter.executeQueryOnEndpoint(endpointId, sparql);
    }
    
    public void refresh() {
        adapter.refreshAllEndpoints();
    }
    
    public void shutdown() {
        adapter.shutdown();
    }
}
```

---

## Configuration: Materialization Policies

### SchemaMaterializationPolicy
Controls how endpoint schemas are discovered and cached.

**Strategies:**
- `REALTIME` - Introspect schema on every access (no caching)
- `CACHED` - Cache schema after first discovery
- `WRITE_THROUGH` - Cache with automatic refresh on data mutations

**Configuration:**
```properties
materialization.schema.strategy=REALTIME
# or
materialization.schema.strategy=CACHED
materialization.schema.cache.ttl=3600  # seconds
# or
materialization.schema.strategy=WRITE_THROUGH
materialization.schema.refresh.interval=300  # seconds
```

### DataMaterializationPolicy
Controls how virtual data is queried and cached.

**Strategies:**
- `REALTIME` - Query endpoint on every SPARQL query (no caching)
- `CACHED` - Cache results after first query
- `WRITE_THROUGH` - Cache results, refresh on mutations

**Configuration:**
```properties
materialization.data.strategy=REALTIME
# or
materialization.data.strategy=CACHED
materialization.data.cache.ttl=600      # seconds
materialization.data.cache.max.size=1000 # max bindings
# or
materialization.data.strategy=WRITE_THROUGH
materialization.data.refresh.on.mutation=true
```

---

## Complete Example: Setting Up Virtual Federation

### Step 1: Define Virtual Endpoints

```java
Map<String, String> jdbcProps = new HashMap<>();
jdbcProps.put("source.type", "JDBC");
jdbcProps.put("jdbc.url", "jdbc:mysql://localhost:3306/db");
jdbcProps.put("jdbc.driver", "com.mysql.jdbc.Driver");
jdbcProps.put("materialization.schema.strategy", "CACHED");
jdbcProps.put("materialization.schema.cache.ttl", "3600");
jdbcProps.put("materialization.data.strategy", "CACHED");
jdbcProps.put("materialization.data.cache.ttl", "600");

FedXEndpoint jdbcEndpoint = new FedXEndpoint() {
    @Override public String nodeId() { return "mysql-db"; }
    @Override public Map<String, String> properties() { return jdbcProps; }
};

Map<String, String> restProps = new HashMap<>();
restProps.put("source.type", "REST");
restProps.put("rest.base.url", "https://api.example.com/v1");
restProps.put("materialization.schema.strategy", "REALTIME");
restProps.put("materialization.data.strategy", "REALTIME");

FedXEndpoint restEndpoint = new FedXEndpoint() {
    @Override public String nodeId() { return "rest-api"; }
    @Override public Map<String, String> properties() { return restProps; }
};
```

### Step 2: Initialize Adapter

```java
VirtualGraphMaterializationRegistry registry = 
    new VirtualGraphMaterializationRegistry(schemaFactory, dataFactory);

VirtualFedXRepositoryMaterializationAdapter adapter =
    new VirtualFedXRepositoryMaterializationAdapter(registry);

List<FedXEndpoint> endpoints = Arrays.asList(jdbcEndpoint, restEndpoint);
adapter.initialize(endpoints);

System.out.println("Initialized with " + adapter.getEndpointCount() + " endpoints");
```

### Step 3: Execute Federated Queries

```java
// Route query to specific endpoint
List<BindingSet> results = adapter.executeQueryOnEndpoint(
    "mysql-db", 
    "SELECT * FROM users WHERE age > 18");

// Retrieve schema
SourceSchema schema = adapter.getEndpointSchema("rest-api");

// Check cache status
if (adapter.isEndpointSchemaCached("mysql-db")) {
    Duration age = adapter.getEndpointSchemaCacheAge("mysql-db");
    System.out.println("Schema cached for " + age.getSeconds() + " seconds");
}
```

### Step 4: Monitor System Health

```java
// Get per-endpoint metrics
Map<String, Object> metrics = adapter.getEndpointMetrics("mysql-db");
System.out.println("Schema fetches: " + metrics.get("schema.fetches"));
System.out.println("Query executions: " + metrics.get("query.executions"));
System.out.println("Query latency (ms): " + metrics.get("query.latency.ms"));

// Get system-wide metrics
Map<String, Map<String, Object>> systemMetrics = adapter.getSystemMetrics();
systemMetrics.forEach((endpointId, m) -> {
    System.out.println(endpointId + ": " + m.get("query.executions") + " queries");
});
```

### Step 5: Manage Lifecycle

```java
// Periodic refresh (call every hour)
adapter.refreshAllEndpoints();

// Clear caches (on demand)
adapter.clearAllCaches();

// Graceful shutdown
adapter.shutdown();
```

---

## Testing

### Unit Testing

Test individual managers:

```java
@Test
void testManagerMetrics() throws Exception {
    VirtualGraphMaterializationManager manager = 
        new VirtualGraphMaterializationManager(endpoint, schema, data);
    
    manager.executeQuery("SELECT ...");
    
    Map<String, Object> metrics = manager.getMetrics();
    assertEquals(1L, metrics.get("query.executions"));
}
```

### Integration Testing

Test registry and adapter:

```java
@Test
void testRegistryDiscovery() throws Exception {
    VirtualGraphMaterializationRegistry registry = 
        new VirtualGraphMaterializationRegistry(schemaFactory, dataFactory);
    
    VirtualGraphMaterializationManager mgr = registry.registerEndpoint(endpoint);
    
    assertNotNull(mgr);
    assertEquals("endpoint-id", mgr.getEndpoint().nodeId());
}

@Test
void testAdapterLifecycle() throws Exception {
    VirtualFedXRepositoryMaterializationAdapter adapter = 
        new VirtualFedXRepositoryMaterializationAdapter(registry);
    
    adapter.initialize(Arrays.asList(endpoint1, endpoint2));
    assertEquals(2, adapter.getEndpointCount());
    
    adapter.shutdown();
    assertEquals(0, adapter.getEndpointCount());
}
```

---

## Performance Tuning

### Schema Caching
```properties
# Cache strategy: balance freshness with performance
materialization.schema.strategy=CACHED
materialization.schema.cache.ttl=3600  # 1 hour

# For rapidly-changing schemas
materialization.schema.cache.ttl=300   # 5 minutes

# For stable schemas
materialization.schema.cache.ttl=86400 # 1 day
```

### Data Caching
```properties
# Cache strategy: balance freshness with performance
materialization.data.strategy=CACHED
materialization.data.cache.ttl=600          # 10 minutes
materialization.data.cache.max.size=10000   # max 10k bindings

# For real-time data
materialization.data.cache.ttl=60            # 1 minute

# For batch processing
materialization.data.cache.ttl=3600          # 1 hour
materialization.data.cache.max.size=100000   # larger cache
```

---

## Error Handling

All operations throw `MaterializationException`:

```java
try {
    SourceSchema schema = adapter.getEndpointSchema("unknown-endpoint");
} catch (MaterializationException e) {
    logger.error("Failed to retrieve schema: {}", e.getMessage());
}

try {
    List<BindingSet> results = adapter.executeQueryOnEndpoint("db", query);
} catch (MaterializationException e) {
    logger.error("Query execution failed: {}", e.getMessage());
}
```

Individual endpoint failures don't break system:

```java
// Even if one endpoint fails, refreshAll continues
adapter.refreshAllEndpoints();  // Logs warnings for individual failures

// Even if one endpoint fails, system metrics are still aggregated
Map<String, Map<String, Object>> metrics = adapter.getSystemMetrics();
// Only failed endpoints have incomplete metrics
```

---

## Production Checklist

- [ ] Endpoint policies configured (schema and data strategies)
- [ ] Cache TTLs tuned for your workload
- [ ] Monitoring/alerting configured for system metrics
- [ ] Periodic refresh scheduled (e.g., hourly)
- [ ] Cache clearing strategy defined (on demand, periodic)
- [ ] Error handling implemented for query failures
- [ ] Shutdown sequence integrated with repository lifecycle
- [ ] Load testing completed with multiple concurrent queries
- [ ] Documentation updated for operations team

---

## See Also

- [PHASE_3C_COMPLETION_SUMMARY.md](./PHASE_3C_COMPLETION_SUMMARY.md) - Architecture and implementation details
- [RepositoryMaterializationIntegrationExample.java](./src/test/java/.../examples/RepositoryMaterializationIntegrationExample.java) - 8 executable examples
- Test files for complete usage patterns

