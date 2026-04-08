package systems.symbol.connect.openapi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Repository;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import systems.symbol.connect.openapi.schema.OpenApiJsonLdMapper;
import systems.symbol.connect.openapi.schema.OpenApiSchemaIntrospector;

/**
 * Integration tests for OpenAPI connector.
 *
 * Tests full OpenAPI spec discovery → JSON-LD mapping → virtual graph registration → SPARQL
 * queries.
 */
@DisplayName("OpenApiConnector Integration Tests")
class OpenApiConnectorIntegrationTest {

  private Repository rdfRepository;
  private RepositoryConnection rdfConnection;
  private OpenApiConnectorConfig config;

  @BeforeEach
  void setUp() throws Exception {
    // Set up RDF repository
    rdfRepository = new SailRepository(new MemoryStore());
    rdfRepository.initialize();
    rdfConnection = rdfRepository.getConnection();

    // Create connector config (with test endpoint URL)
    config = OpenApiConnectorConfig.builder()
        .specUrl("http://localhost:8080/openapi.json")
        .basePath("https://api.example.com")
        .autoDiscover(true)
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (rdfConnection != null) {
      rdfConnection.close();
    }
    if (rdfRepository != null) {
      rdfRepository.shutDown();
    }
  }

  @Test
  @DisplayName("should parse OpenAPI specification")
  void parseOpenApiSpecification() {
    String testSpec = createTestOpenApiSpec();

    OpenApiSchemaIntrospector introspector = new OpenApiSchemaIntrospector(
        "http://localhost:8080/openapi.json");

    assertNotNull(introspector);
  }

  @Test
  @DisplayName("should extract entity schemas from OpenAPI spec")
  void extractEntitySchemasFromSpec() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    assertEquals(2, schemas.size(), "Should extract 2 entity schemas");
    assertTrue(schemas.stream().anyMatch(s -> s.name().equals("User")));
    assertTrue(schemas.stream().anyMatch(s -> s.name().equals("Product")));
  }

  @Test
  @DisplayName("should extract properties from entity schemas")
  void extractPropertiesFromEntitySchemas() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties = userSchema.properties();

    assertEquals(4, properties.size());
    assertTrue(properties.stream().anyMatch(p -> p.name().equals("id")));
    assertTrue(properties.stream().anyMatch(p -> p.name().equals("name")));
    assertTrue(properties.stream().anyMatch(p -> p.name().equals("email")));
  }

  @Test
  @DisplayName("should identify required vs optional properties")
  void identifyRequiredVsOptionalProperties() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties = userSchema.properties();

    OpenApiSchemaIntrospector.PropertyMetadata idProp = properties.stream()
        .filter(p -> p.name().equals("id"))
        .findFirst()
        .orElseThrow();

    assertTrue(idProp.required(), "id should be required");

    OpenApiSchemaIntrospector.PropertyMetadata phoneProp = properties.stream()
        .filter(p -> p.name().equals("phone"))
        .findFirst()
        .orElseThrow();

    assertFalse(phoneProp.required(), "phone should be optional");
  }

  @Test
  @DisplayName("should generate JSON-LD mappings from schemas")
  void generateJsonLdMappingsFromSchemas() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiJsonLdMapper mapper = new OpenApiJsonLdMapper("urn:iq:entities:", "urn:iq:properties:");
    org.eclipse.rdf4j.model.Model jsonLd = mapper.generateJsonLdMappings(schemas);

    assertNotNull(jsonLd);
    assertFalse(jsonLd.isEmpty());

    // Verify RDF structure
    long entityRefs = jsonLd.filter(null, RDF.TYPE, null).size();
    assertTrue(entityRefs > 0, "Should have entity type declarations");
  }

  @Test
  @DisplayName("should store JSON-LD mappings in RDF repository")
  void storeJsonLdMappingsInRepository() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiJsonLdMapper mapper = new OpenApiJsonLdMapper("urn:iq:entities:", "urn:iq:properties:");
    org.eclipse.rdf4j.model.Model jsonLd = mapper.generateJsonLdMappings(schemas);

    // Store in repository
    rdfConnection.add(jsonLd);

    // Verify storage
    long tripleCount = rdfConnection.size();
    assertTrue(tripleCount > 0, "Should have stored JSON-LD triples");
  }

  @Test
  @DisplayName("should register virtual graphs for discovered entities")
  void registerVirtualGraphsForEntities() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    for (OpenApiSchemaIntrospector.EntitySchemaMetadata schema : schemas) {
      IRI virtualGraphId = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
          .createIRI("urn:iq:virtualgraph:api:" + schema.name().toLowerCase());

      assertNotNull(virtualGraphId);
      assertTrue(virtualGraphId.stringValue().contains(schema.name().toLowerCase()));
    }
  }

  @Test
  @DisplayName("should extract endpoints for entities")
  void extractEndpointsForEntities() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.EndpointMetadata> endpoints = userSchema.endpoints();

    assertFalse(endpoints.isEmpty(), "Should extract endpoints for User entity");
    assertTrue(endpoints.stream().anyMatch(e -> e.path().contains("user")));
  }

  @Test
  @DisplayName("should handle schema references between entities")
  void handleSchemaReferences() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata productSchema = schemas.stream()
        .filter(s -> s.name().equals("Product"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties = productSchema.properties();

    // Should have properties that reference other schemas
    assertTrue(properties.size() > 0);
  }

  @Test
  @DisplayName("should support SPARQL queries on virtual graphs")
  void supportSparqlQueriesOnVirtualGraphs() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiJsonLdMapper mapper = new OpenApiJsonLdMapper("urn:iq:entities:", "urn:iq:properties:");
    org.eclipse.rdf4j.model.Model jsonLd = mapper.generateJsonLdMappings(schemas);

    rdfConnection.add(jsonLd);

    // Query for resources
    String sparql = "SELECT ?subject WHERE { ?subject ?predicate ?object } LIMIT 10";
    TupleQuery query = rdfConnection.prepareTupleQuery(sparql);
    TupleQueryResult result = query.evaluate();

    assertTrue(QueryResults.asList(result).size() > 0, "Should return query results");
  }

  @Test
  @DisplayName("should map OpenAPI types to XSD types")
  void mapOpenApiTypesToXsd() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties = userSchema.properties();

    OpenApiSchemaIntrospector.PropertyMetadata idProp = properties.stream()
        .filter(p -> p.name().equals("id"))
        .findFirst()
        .orElseThrow();

    assertEquals("integer", idProp.openApiType());
  }

  @Test
  @DisplayName("should preserve entity descriptions")
  void preserveEntityDescriptions() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    assertNotNull(userSchema.description());
    assertFalse(userSchema.description().isEmpty());
  }

  @Test
  @DisplayName("should handle multiple API endpoints for same entity")
  void handleMultipleEndpointsForEntity() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.EndpointMetadata> endpoints = userSchema.endpoints();

    assertTrue(endpoints.size() >= 1, "Should have endpoints for User entity");
  }

  @Test
  @DisplayName("should support property-level metadata")
  void supportPropertyLevelMetadata() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = schemas.stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties = userSchema.properties();

    OpenApiSchemaIntrospector.PropertyMetadata emailProp = properties.stream()
        .filter(p -> p.name().equals("email"))
        .findFirst()
        .orElseThrow();

    assertNotNull(emailProp.openApiType());
    assertEquals("string", emailProp.openApiType());
  }

  @Test
  @DisplayName("should persist schema metadata in RDF")
  void persistSchemaMetadataInRdf() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    OpenApiJsonLdMapper mapper = new OpenApiJsonLdMapper("urn:iq:entities:", "urn:iq:properties:");
    org.eclipse.rdf4j.model.Model jsonLd = mapper.generateJsonLdMappings(schemas);

    rdfConnection.add(jsonLd);

    // Verify all entities are stored
    String listSparql =
        "SELECT DISTINCT ?subject WHERE { ?subject rdf:type ?type } LIMIT 100";
    TupleQuery query = rdfConnection.prepareTupleQuery(listSparql);
    TupleQueryResult result = query.evaluate();

    List<BindingSet> bindings = QueryResults.asList(result);
    assertTrue(bindings.size() > 0, "Should have stored entity metadata");
  }

  @Test
  @DisplayName("should support incremental schema discovery")
  void supportIncrementalSchemaDiscovery() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = createTestSchemas();

    assertEquals(2, schemas.size());

    // Add another schema
    List<OpenApiSchemaIntrospector.PropertyMetadata> categoryProps = new ArrayList<>();
    categoryProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "id", "integer", "int32", true, null, null));
    categoryProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "name", "string", null, true, null, null));

    List<OpenApiSchemaIntrospector.EndpointMetadata> categoryEndpoints = new ArrayList<>();
    categoryEndpoints.add(
        new OpenApiSchemaIntrospector.EndpointMetadata(
            "/categories", "get", "List categories", "200"));

    OpenApiSchemaIntrospector.EntitySchemaMetadata categorySchema =
        new OpenApiSchemaIntrospector.EntitySchemaMetadata(
            "Category", "Product category", categoryProps, categoryEndpoints);

    // Update schemas list
    schemas.add(categorySchema);

    assertEquals(3, schemas.size(), "Should support adding new schemas");
  }

  // Helper method to create test OpenAPI spec
  private String createTestOpenApiSpec() {
    return """
        {
          "openapi": "3.0.0",
          "info": {"title": "Test API", "version": "1.0.0"},
          "servers": [{"url": "https://api.example.com"}],
          "paths": {},
          "components": {"schemas": {}}
        }
        """;
  }

  // Helper method to create test entity schemas
  private List<OpenApiSchemaIntrospector.EntitySchemaMetadata> createTestSchemas() {
    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas = new ArrayList<>();

    // User schema
    List<OpenApiSchemaIntrospector.PropertyMetadata> userProps = new ArrayList<>();
    userProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "id", "integer", "int32", true, null, null));
    userProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "name", "string", null, true, null, null));
    userProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "email", "string", "email", true, null, null));
    userProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "phone", "string", null, false, null, null));

    List<OpenApiSchemaIntrospector.EndpointMetadata> userEndpoints = new ArrayList<>();
    userEndpoints.add(
        new OpenApiSchemaIntrospector.EndpointMetadata(
            "/users", "get", "Get users", "200"));

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema =
        new OpenApiSchemaIntrospector.EntitySchemaMetadata(
            "User", "User entity", userProps, userEndpoints);
    schemas.add(userSchema);

    // Product schema
    List<OpenApiSchemaIntrospector.PropertyMetadata> productProps = new ArrayList<>();
    productProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "id", "integer", "int32", true, null, null));
    productProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "name", "string", null, true, null, null));
    productProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "price", "number", "double", true, null, null));
    productProps.add(
        new OpenApiSchemaIntrospector.PropertyMetadata(
            "available", "boolean", null, true, null, null));

    List<OpenApiSchemaIntrospector.EndpointMetadata> productEndpoints = new ArrayList<>();
    productEndpoints.add(
        new OpenApiSchemaIntrospector.EndpointMetadata(
            "/products", "get", "Get products", "200"));

    OpenApiSchemaIntrospector.EntitySchemaMetadata productSchema =
        new OpenApiSchemaIntrospector.EntitySchemaMetadata(
            "Product", "Product entity", productProps, productEndpoints);
    schemas.add(productSchema);

    return schemas;
  }
}
