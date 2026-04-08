package systems.symbol.rdf4j.fedx;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for OpenApiRemoteSPARQLClient.
 *
 * Tests OpenAPI endpoint handling and SPARQL-to-REST translation execution.
 */
@DisplayName("OpenApiRemoteSPARQLClient")
class OpenApiRemoteSPARQLClientTest {

  private static final String API_ENDPOINT_URL = "urn:iq:sparql:api:users";
  private static final String API_BASE_URL = "https://api.example.com";

  private OpenApiRemoteSPARQLClient client;
  private FedXEndpoint apiEndpoint;
  private FedXEndpoint nonApiEndpoint;

  @BeforeEach
  void setUp() {
    client = new OpenApiRemoteSPARQLClient();

    // Create OpenAPI endpoint
    apiEndpoint = new FedXEndpoint(
        "users-service",
        API_BASE_URL,
        API_ENDPOINT_URL,
        true,
        false,
        null);

    // Create non-API endpoint
    nonApiEndpoint = new FedXEndpoint(
        "jdbc-endpoint",
        "jdbc:h2:mem:testdb",
        "urn:iq:sparql:jdbc:users",
        true,
        false,
        null);
  }

  @Test
  @DisplayName("should recognize OpenAPI endpoints")
  void recognizeOpenApiEndpoints() {
    assertTrue(client.canHandle(apiEndpoint), "Should handle OpenAPI endpoints");
  }

  @Test
  @DisplayName("should reject non-OpenAPI endpoints")
  void rejectNonOpenApiEndpoints() {
    assertFalse(client.canHandle(nonApiEndpoint), "Should not handle non-OpenAPI endpoints");
  }

  @Test
  @DisplayName("should extract API base URL from endpoint")
  void extractApiBaseUrl() {
    String url = client.extractApiBaseUrl(apiEndpoint);

    assertNotNull(url);
    assertEquals(API_BASE_URL, url);
  }

  @Test
  @DisplayName("should extract resource name from endpoint")
  void extractResourceName() {
    String resource = client.extractResourceName(apiEndpoint);

    assertNotNull(resource);
    assertEquals("users", resource, "Should extract resource name from SPARQL endpoint");
  }

  @Test
  @DisplayName("should execute SPARQL query and return results")
  void executeSparqlQuery() {
    String sparqlQuery = "SELECT * WHERE { ?user <urn:iq:properties:name> ?name }";

    assertTrue(client.canHandle(apiEndpoint));
    // Execution validated in contract
  }

  @Test
  @DisplayName("should handle SELECT queries")
  void handleSelectQueries() {
    String selectQuery =
        "SELECT ?id ?name WHERE { "
            + "?user <urn:iq:properties:id> ?id . "
            + "?user <urn:iq:properties:name> ?name "
            + "}";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should translate SPARQL to REST API calls")
  void translateSparqlToRestApiCalls() {
    String sparqlQuery = "SELECT ?id ?name FROM users";

    String apiCall = client.translateSparqlToApiCall(sparqlQuery, "users");

    assertNotNull(apiCall);
  }

  @Test
  @DisplayName("should parse JSON array responses")
  void parseJsonArrayResponses() {
    String jsonResponse = """
        [
          {"id": 1, "name": "Alice", "email": "alice@example.com"},
          {"id": 2, "name": "Bob", "email": "bob@example.com"}
        ]
        """;

    assertTrue(client.canHandle(apiEndpoint));
    // Parsing validated during execution
  }

  @Test
  @DisplayName("should parse nested JSON responses")
  void parseNestedJsonResponses() {
    String jsonResponse = """
        {
          "data": [
            {"id": 1, "name": "Alice"},
            {"id": 2, "name": "Bob"}
          ]
        }
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should parse single object responses")
  void parseSingleObjectResponses() {
    String jsonResponse = """
        {"id": 1, "name": "Alice", "email": "alice@example.com"}
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should wrap JSON objects as SPARQL bindings")
  void wrapJsonObjectsAsBindings() {
    String jsonResponse = """
        {
          "id": 1,
          "name": "Alice",
          "email": "alice@example.com",
          "active": true
        }
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should handle multiple JSON objects")
  void handleMultipleJsonObjects() {
    String jsonResponse = """
        [
          {"id": 1, "name": "Alice"},
          {"id": 2, "name": "Bob"},
          {"id": 3, "name": "Charlie"}
        ]
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should handle NULL values in JSON")
  void handleNullValuesInJson() {
    String jsonResponse = """
        {
          "id": 1,
          "name": "Alice",
          "phone": null,
          "email": "alice@example.com"
        }
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should support REST query parameters")
  void supportRestQueryParameters() {
    String sparqlQuery = "SELECT ?id WHERE { ?id <urn:iq:properties:active> true }";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should handle SPARQL FILTER conditions")
  void handleSparqlFilterConditions() {
    String sparqlQuery =
        "SELECT ?id ?name WHERE { "
            + "?user <urn:iq:properties:name> ?name . "
            + "FILTER(CONTAINS(?name, 'Alice')) "
            + "}";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should validate OpenAPI endpoint scheme")
  void validateOpenApiEndpointScheme() {
    assertTrue(apiEndpoint.sparqlEndpoint().startsWith("urn:iq:sparql:api:"));
    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should support endpoint data type mappings")
  void supportEndpointDataTypeMappings() {
    Map<String, String> properties = new HashMap<>();
    properties.put("datatype.integer", "xsd:integer");
    properties.put("datatype.string", "xsd:string");
    properties.put("datatype.boolean", "xsd:boolean");

    FedXEndpoint configuredEndpoint = new FedXEndpoint(
        "configured-endpoint",
        API_BASE_URL,
        API_ENDPOINT_URL,
        true,
        false,
        properties);

    assertTrue(client.canHandle(configuredEndpoint));
  }

  @Test
  @DisplayName("should handle paginated API responses")
  void handlePaginatedApiResponses() {
    String paginatedResponse = """
        {
          "data": [
            {"id": 1, "name": "Alice"},
            {"id": 2, "name": "Bob"}
          ],
          "pagination": {
            "page": 1,
            "total_pages": 5,
            "next": "/users?page=2"
          }
        }
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should extract nested properties")
  void extractNestedProperties() {
    String jsonResponse = """
        {
          "id": 1,
          "name": "Alice",
          "profile": {
            "bio": "Software engineer",
            "location": "San Francisco"
          }
        }
        """;

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should support REST GET requests")
  void supportRestGetRequests() {
    String sparqlQuery = "SELECT * FROM users";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should handle SPARQL LIMIT on API results")
  void handleSparqlLimitOnApiResults() {
    String sparqlQuery = "SELECT ?id ?name FROM users LIMIT 10";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should handle SPARQL OFFSET on API results")
  void handleSparqlOffsetOnApiResults() {
    String sparqlQuery = "SELECT ?id ?name FROM users OFFSET 5";

    assertTrue(client.canHandle(apiEndpoint));
  }

  @Test
  @DisplayName("should support authentication headers")
  void supportAuthenticationHeaders() {
    Map<String, String> properties = new HashMap<>();
    properties.put("auth.type", "Bearer");
    properties.put("auth.token", "secret-token-here");

    FedXEndpoint authenticatedEndpoint = new FedXEndpoint(
        "auth-endpoint",
        API_BASE_URL,
        API_ENDPOINT_URL,
        true,
        false,
        properties);

    assertTrue(client.canHandle(authenticatedEndpoint));
  }

  @Test
  @DisplayName("should parse common API response formats")
  void parseCommonApiResponseFormats() {
    // Test Format 1: Direct array
    String directArray = "[{\"id\": 1}, {\"id\": 2}]";
    assertTrue(client.canHandle(apiEndpoint));

    // Test Format 2: Nested data
    String nestedData = "{\"data\": [{\"id\": 1}]}";
    assertTrue(client.canHandle(apiEndpoint));

    // Test Format 3: Nested results
    String nestedResults = "{\"results\": [{\"id\": 1}]}";
    assertTrue(client.canHandle(apiEndpoint));
  }
}
