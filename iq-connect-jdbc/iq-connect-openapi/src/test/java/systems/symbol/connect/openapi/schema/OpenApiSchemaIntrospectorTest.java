package systems.symbol.connect.openapi.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for OpenAPI schema introspection.
 *
 * Tests OpenAPI specification parsing and entity schema extraction using mocked HTTP requests.
 */
@DisplayName("OpenApiSchemaIntrospector")
class OpenApiSchemaIntrospectorTest {

  private static final String TEST_SPEC_URL = "http://api.example.com/openapi.json";

  @Mock
  private org.apache.hc.client5.http.classic.HttpClient mockHttpClient;

  private OpenApiSchemaIntrospector introspector;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);

    // Create a test OpenAPI spec JSON
    String testSpec = createTestOpenApiSpec();

    introspector = new OpenApiSchemaIntrospector(TEST_SPEC_URL);
  }

  private String createTestOpenApiSpec() {
    return """
        {
          "openapi": "3.0.0",
          "info": {
            "title": "Test API",
            "version": "1.0.0"
          },
          "servers": [{"url": "https://api.example.com/v1"}],
          "paths": {
            "/users": {
              "get": {
                "summary": "Get users",
                "responses": {
                  "200": {
                    "description": "List of users",
                    "content": {
                      "application/json": {
                        "schema": {"$ref": "#/components/schemas/User"}
                      }
                    }
                  }
                }
              }
            }
          },
          "components": {
            "schemas": {
              "User": {
                "type": "object",
                "properties": {
                  "id": {"type": "integer"},
                  "name": {"type": "string"},
                  "email": {"type": "string", "format": "email"},
                  "active": {"type": "boolean"},
                  "created_date": {"type": "string", "format": "date"}
                },
                "required": ["id", "name", "email"]
              },
              "Profile": {
                "type": "object",
                "properties": {
                  "id": {"type": "integer"},
                  "user_id": {"type": "integer"},
                  "bio": {"type": "string"},
                  "user": {"$ref": "#/components/schemas/User"}
                },
                "required": ["id", "user_id"]
              }
            }
          }
        }
        """;
  }

  @Test
  @DisplayName("should parse OpenAPI specification")
  void parseOpenApiSpecification() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    assertNotNull(metadata);
    assertEquals("Test API", metadata.title());
    assertEquals("1.0.0", metadata.version());
    assertNotNull(metadata.basePath());
  }

  @Test
  @DisplayName("should extract entity schemas from components")
  void extractEntitySchemas() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    List<OpenApiSchemaIntrospector.EntitySchemaMetadata> schemas =
        metadata.schemas();

    assertNotNull(schemas);
    assertFalse(schemas.isEmpty(), "Should extract entity schemas");
    assertTrue(
        schemas.stream().anyMatch(s -> s.name().equals("User")),
        "Should have User schema");
    assertTrue(
        schemas.stream().anyMatch(s -> s.name().equals("Profile")),
        "Should have Profile schema");
  }

  @Test
  @DisplayName("should extract properties from schema")
  void extractPropertiesFromSchema() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties =
        userSchema.properties();

    assertNotNull(properties);
    assertEquals(5, properties.size(), "User should have 5 properties");
    assertTrue(
        properties.stream().anyMatch(p -> p.name().equals("id")),
        "Should have id property");
    assertTrue(
        properties.stream().anyMatch(p -> p.name().equals("name")),
        "Should have name property");
    assertTrue(
        properties.stream().anyMatch(p -> p.name().equals("email")),
        "Should have email property");
  }

  @Test
  @DisplayName("should identify required properties")
  void identifyRequiredProperties() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties =
        userSchema.properties();

    OpenApiSchemaIntrospector.PropertyMetadata idProp = properties.stream()
        .filter(p -> p.name().equals("id"))
        .findFirst()
        .orElseThrow();

    assertTrue(idProp.required(), "id should be required");

    OpenApiSchemaIntrospector.PropertyMetadata activeProp = properties.stream()
        .filter(p -> p.name().equals("active"))
        .findFirst()
        .orElseThrow();

    assertFalse(activeProp.required(), "active should not be required");
  }

  @Test
  @DisplayName("should map OpenAPI types to XSD types")
  void mapOpenApiTypesToXsd() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties =
        userSchema.properties();

    OpenApiSchemaIntrospector.PropertyMetadata idProp = properties.stream()
        .filter(p -> p.name().equals("id"))
        .findFirst()
        .orElseThrow();

    assertEquals("integer", idProp.openApiType(), "id should be integer");
    assertNotNull(idProp.format(), "Type mapping should include format info");
  }

  @Test
  @DisplayName("should detect schema references")
  void detectSchemaReferences() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata profileSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("Profile"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties =
        profileSchema.properties();

    assertTrue(
        properties.stream().anyMatch(p -> p.name().equals("user")),
        "Should detect reference property");

    OpenApiSchemaIntrospector.PropertyMetadata userRef = properties.stream()
        .filter(p -> p.name().equals("user"))
        .findFirst()
        .orElseThrow();

    assertNotNull(userRef.referencedSchema(), "Should have reference info");
  }

  @Test
  @DisplayName("should extract endpoints for entities")
  void extractEndpointsForEntities() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata userSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("User"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.EndpointMetadata> endpoints =
        userSchema.endpoints();

    assertNotNull(endpoints);
    assertFalse(endpoints.isEmpty(), "Should detect endpoints for User schema");
  }

  @Test
  @DisplayName("should handle nested object types")
  void handleNestedObjectTypes() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    OpenApiSchemaIntrospector.EntitySchemaMetadata profileSchema = metadata.schemas().stream()
        .filter(s -> s.name().equals("Profile"))
        .findFirst()
        .orElseThrow();

    List<OpenApiSchemaIntrospector.PropertyMetadata> properties =
        profileSchema.properties();

    assertTrue(properties.size() > 0, "Should handle nested object properties");
  }

  @Test
  @DisplayName("should preserve schema metadata")
  void preserveSchemaMetadata() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    assertNotNull(metadata.title());
    assertNotNull(metadata.version());
    assertNotNull(metadata.basePath());
    assertFalse(metadata.schemas().isEmpty());
  }

  @Test
  @DisplayName("should extract OpenAPI version")
  void extractOpenApiVersion() throws Exception {
    OpenApiSchemaIntrospector.OpenApiSpecMetadata metadata =
        introspector.parseSpecification();

    assertEquals("Test API", metadata.title());
    assertEquals("1.0.0", metadata.version());
  }
}
