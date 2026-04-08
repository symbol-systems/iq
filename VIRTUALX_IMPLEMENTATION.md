# VIRTUALX Implementation: Clean JDBC and OpenAPI Integration

**Date:** April 8, 2026  
**Status:** Complete implementation with clean separation of concerns  
**Location:** `/developer/iq/`

## Overview

This implementation provides **clean, separate modules** for auto-discovering JDBC database schemas and OpenAPI/Swagger specifications, with a **strategy-based federation layer** for querying both sources transparently via SPARQL.

### Key Design Principles

✅ **Separation of Concerns:** JDBC and OpenAPI logic are completely isolated  
✅ **Single Responsibility:** Each class has one job  
✅ **Strategy Pattern:** Query routing is logic-free dispatch  
✅ **No External Dependencies:** Self-hosted, RDF-native (no Ontop, no external tools)  
✅ **Production Ready:** Proper error handling, logging, and testability

---

## Module Structure

### 1. iq-connect-jdbc
**Purpose:** Auto-discover JDBC databases and generate R2RML mappings

```
iq-connect-jdbc/
├── src/main/java/systems/symbol/connect/jdbc/
│   ├── JdbcConnectorConfig.java              (❶ Configuration - immutable value object)
│   ├── JdbcConnector.java                    (❷ Lifecycle - orchestrates discovery)
│   └── schema/
│       ├── JdbcSchemaIntrospector.java       (❸ Metadata extraction via JDBC API)
│       └── JdbcR2RMLGenerator.java           (❹ RDF triple generation)
├── pom.xml
└── src/test/java/
```

**Key Classes:**

- **`JdbcConnectorConfig`** — Immutable configuration object
  - JDBC URL, credentials, auto-discovery flags
  - Builder pattern for fluent construction

- **`JdbcSchemaIntrospector`** — Pure introspection (no RDF)
  - Uses standard JDBC `DatabaseMetaData` API
  - Extracts tables, columns, types, keys, indexes
  - Returns `TableMetadata`, `ColumnMetadata`, `ForeignKeyMetadata`
  - **Reusable:** can be used outside IQ

- **`JdbcR2RMLGenerator`** — Converts metadata to RDF
  - Generates W3C R2RML triples directly
  - Maps SQL types to XSD types
  - Creates URI templates for subject generation
  - Outputs RDF `Model` (not dependent on IQ repository)

- **`JdbcConnector`** — Orchestrates end-to-end flow
  - Extends `AbstractConnector` lifecycle
  - Calls introspector → generator → storage → virtual graph registration
  - Stores R2RML in named graph `{realm}/schemas/{jdbcHash}`
  - Registers each table as a `VirtualGraph` instance

---

### 2. iq-connect-openapi
**Purpose:** Auto-discover OpenAPI specs and generate JSON-LD mappings

```
iq-connect-openapi/
├── src/main/java/systems/symbol/connect/openapi/
│   ├── OpenApiConnectorConfig.java           (❶ Configuration - immutable value object)
│   ├── OpenApiConnector.java                 (❷ Lifecycle - orchestrates discovery)
│   └── schema/
│       ├── OpenApiSchemaIntrospector.java    (❸ Metadata extraction - uses Swagger parser)
│       └── OpenApiJsonLdMapper.java          (❹ RDF triple generation)
├── pom.xml
└── src/test/java/
```

**Key Classes:**

- **`OpenApiConnectorConfig`** — Immutable configuration object
  - Spec URL, base path, endpoints, auth token
  - Rate limiting and caching flags

- **`OpenApiSchemaIntrospector`** — Pure introspection (no RDF)
  - Uses Swagger/OpenAPI parser library
  - Fetches spec via HTTP
  - Extracts entities, properties, relationships
  - Returns `OpenApiSpecMetadata`, `EntitySchemaMetadata`, `PropertyMetadata`
  - Maps OpenAPI types to XSD types
  - **Reusable:** can be used outside IQ

- **`OpenApiJsonLdMapper`** — Converts metadata to RDF
  - Generates JSON-LD inspired RDF triples
  - Creates entity type definitions with properties
  - Models relationships between entities
  - Outputs RDF `Model` (not dependent on IQ repository)

- **`OpenApiConnector`** — Orchestrates end-to-end flow
  - Extends `AbstractConnector` lifecycle
  - Calls introspector → mapper → storage → virtual graph registration
  - Stores JSON-LD in named graph `{realm}/schemas/{specHash}`
  - Registers each entity as a `VirtualGraph` instance

---

### 3. iq-rdf4j-fedx-extensions
**Purpose:** Strategy-based remote SPARQL client router

```
iq-rdf4j-fedx-extensions/
├── src/main/java/systems/symbol/rdf4j/fedx/
│   ├── I_RemoteSPARQLExecutor.java           (❶ Strategy interface)
│   ├── I_RemoteSPARQLClient.java             (❷ Public API interface)
│   ├── FedXEndpoint.java                     (❸ Endpoint value object)
│   ├── JdbcRemoteSPARQLClient.java           (❹ JDBC strategy - translates SPARQL to SQL)
│   ├── OpenApiRemoteSPARQLClient.java        (❺ OpenAPI strategy - translates SPARQL to REST)
│   ├── RemoteSPARQLClientRouter.java         (❻ Dispatcher - routes to appropriate strategy)
│   └── [Future: GraphQLRemoteSPARQLClient, HttpRemoteSPARQLClient, ...]
├── pom.xml
└── src/test/java/
```

**Key Classes:**

- **`I_RemoteSPARQLExecutor`** — Strategy interface
  - `canHandle(endpoint)` — checks if this executor matches
  - `execute(endpoint, sparqlQuery)` — translates and executes
  - **Each implementation has ONE responsibility**

- **`JdbcRemoteSPARQLClient`** (implements `I_RemoteSPARQLExecutor`)
  - Handles endpoints with scheme `urn:iq:sparql:jdbc:*`
  - Translates SPARQL patterns to SQL WHERE clauses
  - Executes SQL via JDBC
  - Wraps results as SPARQL bindings
  - **No knowledge of OpenAPI, GraphQL, or HTTP endpoints**

- **`OpenApiRemoteSPARQLClient`** (implements `I_RemoteSPARQLExecutor`)
  - Handles endpoints with scheme `urn:iq:sparql:openapi:*`
  - Translates SPARQL patterns to HTTP query parameters
  - Executes REST calls to API
  - Parses JSON responses into SPARQL bindings
  - **No knowledge of JDBC, GraphQL, or HTTP protocol details**

- **`RemoteSPARQLClientRouter`** (implements `I_RemoteSPARQLClient`)
  - **Zero business logic** — purely a dispatcher
  - Registers all strategies in order (specific first, fallback last)
  - Iterates through strategies to find first that `canHandle()`
  - Delegates to matched strategy
  - Easy to extend: add new strategy, register in router

---

### 4. iq-onto/rdf
**Ontologies for schema mapping and virtual graph metadata**

- **`schema-mapping.ttl`** — Metadata for auto-discovered schemas
  - `sm:SchemaMapping` class
  - Properties: `sourceType`, `sourceUri`, `detectedAt`, `generatedBy`, `r2rmlGraph`, `jsonLdGraph`, `approved`
  - Tracks whether mappings have been human-reviewed
  - Supports refinements (user customizations)

- **`virtual-graph.ttl`** — Metadata for virtual graphs
  - `vg:VirtualGraph` class
  - Properties: `sourceType`, `sparqlEndpoint`, capability flags, statistics
  - Tracks health status, query statistics
  - Links to R2RML / JSON-LD mapping graphs

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      IQ Federation Layer                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐      ┌──────────────────────────┐    │
│  │ SPARQL Query     │      │ Federated Query Engine   │    │
│  │ (user-facing)    │──→   │ (optimizer + planner)    │    │
│  └──────────────────┘      └──────────────────────────┘    │
│                                     ↓                        │
│                    ┌──────────────────────────┐             │
│                    │ RemoteSPARQLClientRouter │             │
│                    │  (strategy dispatcher)   │             │
│                    └──────────────────────────┘             │
│                     ↙   ↓   ↓   ↓   ↖                       │
│            ┌─────┐ ┌──────┐ ┌──────┐ ┌──────────┐          │
│            │JRM  │ │JDBC  │ │OpenAPI│GraphQL    │(HTTP)   │
│            │(RDF)│ │Strat │ │Strat  │Strategy   │fallback │
│            └─────┘ └──────┘ └──────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────┘
      ↓           ↓         ↓          ↓
┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐
│  Local  │ │PostgreSQL│ │ Stripe │ │ GraphQL  │
│   RDF   │ │ Database │ │  API   │ │Endpoint  │
│ 4Store  │ │          │ │        │ │          │
└─────────┘ └──────────┘ └────────┘ └──────────┘
      ↓           ↓         ↓          ↓
   [RDF]        [SQL]     [REST]     [GraphQL]
   triples      tables    endpoints   queries
```

## Data Flow: JDBC Discovery

```
1. User registers JDBC connector
   └─ Config: jdbcUrl, user, password, autoDiscover=true

2. JdbcConnector.initialize()
   └─ Creates JdbcSchemaIntrospector + JdbcR2RMLGenerator

3. JdbcConnector.doRefresh()
   
   a) Introspect database schema
      JdbcSchemaIntrospector.introspectTables()
      └─ Uses DatabaseMetaData API
      └─ Returns: List<TableMetadata>
         - Tables: name, schema, comments
         - Columns: name, type, nullability, defaults
         - Keys: PK, FK relationships
         - Indexes: index definitions
   
   b) Generate R2RML mappings
      JdbcR2RMLGenerator.generateR2RML(tables)
      └─ Creates RDF Model with W3C R2RML triples
      └─ For each table:
         - TriplesMap with logicalTable/tableName
         - subjectMap with URI template + type class
         - predicateObjectMap for each column
         - joinConditions for ForeignKeys
   
   c) Store R2RML graph
      Connection.add(model, graphIri)
      └─ Named graph: urn:iq:realm:default/schemas/jdbc-{hash}
   
   d) Register virtual graphs
      For each table, create:
      └─ VirtualGraph instance (RDF triples)
      └─ Links mapping table → schema graph
      └─ Sets capabilities: FilterPushdown, JoinPushdown, etc.

4. Query across sources
   SPARQL: SELECT ?customer ?name ?email WHERE {
     ?customer a ex:Customer ;
               ex:name ?name ;
               ex:email ?email .
   }
   
   a) Federation engine recognizes multiple endpoints
   b) RemoteSPARQLClientRouter routes:
      - JdbcRemoteSPARQLClient handles urn:iq:sparql:jdbc:*
   c) ClientTranslates SPARQL to SQL:
      SELECT c.id, c.name, c.email FROM customers c
   d) Executes SQL, wraps results as SPARQL bindings
   e) Returns to query engine for merging
```

## Data Flow: OpenAPI Discovery

```
1. User registers OpenAPI connector
   └─ Config: specUrl, basePath, endpoints, auth

2. OpenApiConnector.initialize()
   └─ Creates OpenApiSchemaIntrospector + OpenApiJsonLdMapper

3. OpenApiConnector.doRefresh()
   
   a) Parse OpenAPI spec
      OpenApiSchemaIntrospector.parseSpecification()
      └─ Fetches spec via HTTP
      └─ Uses Swagger parser
      └─ Returns: OpenApiSpecMetadata
         - Title, version
         - List<EntitySchemaMetadata>
           - Entity name, type, description
           - Properties: name, type, format, required
           - References to other entities
   
   b) Generate JSON-LD mappings
      OpenApiJsonLdMapper.generateJsonLdMappings(schemas)
      └─ Creates RDF Model with JSON-LD-inspired triples
      └─ For each entity:
         - EntityMapping instance
         - linked to entity type class
         - property definitions with types
         - reference links to related entities
   
   c) Store JSON-LD graph
      Connection.add(model, graphIri)
      └─ Named graph: urn:iq:realm:default/schemas/openapi-{hash}
   
   d) Register virtual graphs
      For each entity, create:
      └─ VirtualGraph instance (RDF triples)
      └─ Links mapping entity → schema graph
      └─ Sets API endpoint URL + capabilities

4. Query across OpenAPI
   SPARQL: SELECT ?charge ?amount ?customerId WHERE {
     ?charge a ex:StripeCharge ;
             ex:amount ?amount ;
             ex:customerId ?customerId .
     FILTER (?amount > 5000) .
   }
   
   a) RemoteSPARQLClientRouter routes:
      - OpenApiRemoteSPARQLClient handles urn:iq:sparql:openapi:*
   b) Client translates SPARQL to REST:
      GET /charges?amount[gt]=5000&limit=100
   c) Parses JSON response, wraps as SPARQL bindings
   d) Returns to query engine
```

## Integration with Federation

The strategy pattern integrates cleanly with existing FedX federation layer:

```java
// In FedXConfiguration or similar:
@Produces
I_RemoteSPARQLClient remoteClient(
    JdbcConnectorConfig jdbcConfig,
    OpenApiConnectorConfig apiConfig) {
  
  return new RemoteSPARQLClientRouter(
    new JdbcRemoteSPARQLClient(jdbcTranslator, r2rmlMapper),
    new OpenApiRemoteSPARQLClient(apiHttpClient),
    new GraphQLRemoteSPARQLClient(graphqlAdapter),
    new HttpRemoteSPARQLClient(httpClient)  // fallback, always matches
  );
}

// Existing federation engine unchanged
// Just calls: remoteClient.executeQuery(endpoint, sparqlQuery)
```

---

## Key Design Decisions

### 1. **Separation of JDBC and OpenAPI**
- ✅ Completely independent modules
- ✅ No cross-dependencies
- ✅ Can be developed/tested/deployed separately
- ✅ Easy to add new connector types (GraphQL, gRPC, etc.)

### 2. **Pure Introspection Classes** (no RDF)
- `JdbcSchemaIntrospector` and `OpenApiSchemaIntrospector` have **zero dependencies on RDF**
- Reusable outside IQ for other projects
- Easy to test with mocks
- Fast to iterate without RDF repository overhead

### 3. **Generator Classes** (RDF output)
- `JdbcR2RMLGenerator` and `OpenApiJsonLdMapper` are **pure functions**
- Input: metadata objects → Output: RDF `Model`
- No side effects, no external I/O
- Easy to test, easy to version

### 4. **Strategy Pattern for Federation**
- ✅ Each executor handles ONE endpoint type
- ✅ Router is **zero-logic dispatcher**
- ✅ Easy to extend (add new strategy, register)
- ✅ Easy to replace (mock strategy for testing)
- ✅ Follows Interface Segregation Principle (small interfaces)

### 5. **Named Graphs for Mappings**
- Schema mappings stored as RDF (not YAML/JSON files)
- Discoverable and queryable alongside data
- Versioned with repo
- Can be refined / approved by humans

### 6. **Virtual Graphs as RDF Instances**
- Each discovered table/entity becomes a `VirtualGraph` RDF instance
- Linked to schema mapping graphs
- Trackable (health, statistics, last queried)
- Queryable for introspection

---

## Testing Strategy

Each module can be tested independently:

### iq-connect-jdbc Tests
```java
// Test introspection (no RDF needed)
@Test
void introspectPostgresa() throws SQLException {
  JdbcSchemaIntrospector intro = new JdbcSchemaIntrospector(h2In-memory);
  List<TableMetadata> tables = intro.introspectTables();
  
  assertEquals(2, tables.size());
  TableMetadata customers = tables.get(0);
  assertEquals("customers", customers.getTableName());
  assertEquals(5, customers.getColumns().size());
}

// Test R2RML generation (no JDBC needed)
@Test
void generateR2RML() {
  JdbcR2RMLGenerator gen = new JdbcR2RMLGenerator("urn:iq:db:", "urn:iq:schemas:");
  Model r2rml = gen.generateR2RML(mockTables);
  
  assertTrue(r2rml.contains(null, RDF.TYPE, createIRI(R2RML_NS + "TriplesMap")));
  assertEquals(expectedTripleCount, r2rml.size());
}
```

### iq-connect-openapi Tests
```java
// Test introspection (no RDF needed)
@Test
void introspectOpenAPI() throws Exception {
  // Use mock HTTP client or WireMock for spec
  OpenApiSchemaIntrospector intro = new OpenApiSchemaIntrospector(specUrl);
  OpenApiSpecMetadata metadata = intro.parseSpecification();
  
  assertEquals(3, metadata.getEntitySchemas().size());
}

// Test JSON-LD generation (no HTTP needed)
@Test
void generateJsonLd() {
  OpenApiJsonLdMapper mapper = new OpenApiJsonLdMapper("urn:iq:apis:", "urn:iq:entities:");
  Model jsonLd = mapper.generateJsonLdMappings(schemas);
  
  assertTrue(jsonLd.contains(null, RDF.TYPE, createIRI(JSONLD_NS + "EntityMapping")));
}
```

### iq-rdf4j-fedx-extensions Tests
```java
// Test router (mock strategies)
@Test
void routerSelectsCorrectStrategy() {
  I_RemoteSPARQLExecutor jdbc = mock(I_RemoteSPARQLExecutor.class);
  I_RemoteSPARQLExecutor api = mock(I_RemoteSPARQLExecutor.class);
  
  when(jdbc.canHandle(endpoint)).thenReturn(false);
  when(api.canHandle(endpoint)).thenReturn(true);
  when(api.execute(endpoint, query)).thenReturn(results);
  
  RemoteSPARQLClientRouter router = new RemoteSPARQLClientRouter(jdbc, api);
  TupleQueryResult result = router.executeQuery(endpoint, query);
  
  verify(api).execute(endpoint, query);
  verify(jdbc, never()).execute(any(), any());
}

// Test JDBC strategy (mock JDBC connection)
@Test
void jdbcStrategyTranslatesSparql() {
  JdbcRemoteSPARQLClient client = new JdbcRemoteSPARQLClient();
  assertTrue(client.canHandle(endpointWithJdbcScheme));
  
  TupleQueryResult result = client.execute(endpoint, sparqlQuery);
  assertNotNull(result);
}
```

---

## Future Extensions

### Phase 2: GraphQL Support
```java
public class GraphQLRemoteSPARQLClient implements I_RemoteSPARQLExecutor {
  @Override
  public boolean canHandle(FedXEndpoint ep) {
    return ep.getSparqlEndpoint().startsWith("urn:iq:sparql:graphql:");
  }
  
  @Override
  public TupleQueryResult execute(FedXEndpoint ep, String sparql) {
    // Translate SPARQL to GraphQL queries
    // Execute via graphql-java client
    // Return as SPARQL bindings
  }
}
// Register in router: new RemoteSPARQLClientRouter(..., new GraphQLRemoteSPARQLClient(), ...)
```

### Phase 3: Query Optimization
```java
// Cost-based planning using statistics from virtual graphs
SimpleFederatedQueryOptimizer.selectBestOrderBoundJoins(
  JoinArgument join,
  VirtualGraphStatistics stats  // from vg:* properties
);
```

### Phase 4: Schema Evolution
```java
// Detect when source schema changes, auto-update mappings
ChangeDetector.hasSchemaChanged(oldMapping, newIntrospection)
  ? RegenerateAndNotify()
  : NoAction()
```

---

## Build & Deploy

### Build all modules
```bash
./mvnw clean install -pl iq-connect-jdbc,iq-connect-openapi,iq-rdf4j-fedx-extensions -am
```

### Run tests
```bash
./mvnw test -pl iq-connect-jdbc
./mvnw test -pl iq-connect-openapi
./mvnw test -pl iq-rdf4j-fedx-extensions
```

### Integration test
```bash
./mvnw verify -DskipITs=false -pl iq-apis -am
```

---

## References

- **W3C R2RML Standard:** https://www.w3.org/TR/r2rml/
- **OpenAPI 3.0 Spec:** https://spec.openapis.org/oas/v3.0.0
- **SPARQL 1.1 Query Language:** https://www.w3.org/TR/sparql11-query/
- **Strategy Pattern:** https://refactoring.guru/design-patterns/strategy
- **Federation Queries (FedX):** https://rdf4j.org/documentation/reference/fedx/

---

## Summary

This implementation demonstrates **clean architecture principles**:

- ✅ **Modularity:** JDBC and OpenAPI are separate, independently testable modules
- ✅ **Separation of Concerns:** Each class has one responsibility
- ✅ **Design Patterns:** Strategy pattern for extensible query dispatch
- ✅ **No Duplication:** Shared interfaces (I_RemoteSPARQLExecutor) reduce boilerplate
- ✅ **Testability:** Pure functions, mockable dependencies, isolated concerns
- ✅ **Extensibility:** Add new query types (GraphQL, gRPC) without modifying existing code
- ✅ **Production Ready:** Error handling, logging, health tracking, statistics

The VIRTUALX architecture maintains IQ's **self-hosted principle** while providing transparent querying across SQL databases, REST APIs, and RDF repositories — all orchestrated through a clean, federated SPARQL layer.
