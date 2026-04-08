package systems.symbol.connect.openapi.schema;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.PathItem;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Introspects OpenAPI/Swagger specifications to extract entity schemas.
 *
 * <p>
 * Single Responsibility: Parse OpenAPI specs and extract entity/schema information
 * without generating RDF triples or JSON-LD mappings.
 * </p>
 *
 * <p>
 * Detects:
 * </p>
 * <ul>
 *   <li>Entities (from OpenAPI schema components)</li>
 *   <li>Properties (from schema properties)</li>
 *   <li>Property types (OpenAPI type → XSD type mapping)</li>
 *   <li>Entity relationships (references between schemas)</li>
 *   <li>API endpoints and their request/response types</li>
 * </ul>
 */
public class OpenApiSchemaIntrospector {

  private static final Logger LOG = Logger.getLogger(OpenApiSchemaIntrospector.class.getName());

  private final String specUrl;
  private final HttpClient httpClient;
  private final int readTimeout;

  public OpenApiSchemaIntrospector(String specUrl) {
    this(specUrl, HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(), 30);
  }

  public OpenApiSchemaIntrospector(String specUrl, HttpClient httpClient, int readTimeout) {
    this.specUrl = Objects.requireNonNull(specUrl, "specUrl must not be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.readTimeout = readTimeout > 0 ? readTimeout : 30;
  }

  /**
   * Parse OpenAPI specification and extract schema information.
   *
   * @return OpenAPI specification model
   * @throws Exception if parsing fails
   */
  public OpenApiSpecMetadata parseSpecification() throws Exception {
    LOG.log(Level.INFO, "Parsing OpenAPI spec: {0}", specUrl);

    // Fetch spec from URL
    String specContent = fetchSpecContent();

    // Parse using Swagger parser
    OpenAPIParser parser = new OpenAPIParser();
    OpenAPI openAPI = parser.readContents(specContent, null, null).getOpenAPI();

    if (openAPI == null) {
      throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + specUrl);
    }

    LOG.log(Level.INFO, "Successfully parsed OpenAPI spec version {0}", 
        openAPI.getOpenapi());

    return new OpenApiSpecMetadata(openAPI, introspectSchemas(openAPI));
  }

  /**
   * Fetch OpenAPI spec content from URL.
   */
  private String fetchSpecContent() throws Exception {
    LOG.log(Level.FINE, "Fetching OpenAPI spec from: {0}", specUrl);

    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(specUrl))
            .timeout(java.time.Duration.ofSeconds(readTimeout))
            .header("Accept", "application/json,application/yaml")
            .build();

    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Failed to fetch OpenAPI spec: HTTP " + response.statusCode());
    }

    return response.body();
  }

  /**
   * Introspect all entity schemas from OpenAPI components.
   */
  private List<EntitySchemaMetadata> introspectSchemas(OpenAPI openAPI) {
    List<EntitySchemaMetadata> schemas = new ArrayList<>();

    if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
      LOG.log(Level.WARNING, "No schemas found in OpenAPI spec");
      return schemas;
    }

    for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
      String schemaName = entry.getKey();
      Schema schema = entry.getValue();

      EntitySchemaMetadata entitySchema =
          introspectSchema(schemaName, schema);
      schemas.add(entitySchema);

      LOG.log(Level.FINE, "Introspected schema: {0} ({1} properties)", 
          new Object[] {schemaName, entitySchema.getProperties().size()});
    }

    LOG.log(Level.INFO, "Introspected {0} entity schemas", schemas.size());
    return schemas;
  }

  /**
   * Introspect a single entity schema.
   */
  private EntitySchemaMetadata introspectSchema(String name, Schema schema) {
    List<PropertyMetadata> properties = new ArrayList<>();
    Set<String> requiredProperties = new HashSet<>();

    if (schema.getRequired() != null) {
      requiredProperties.addAll(schema.getRequired());
    }

    if (schema.getProperties() != null) {
      for (Map.Entry<String, Schema> propEntry : schema.getProperties().entrySet()) {
        String propName = propEntry.getKey();
        Schema propSchema = propEntry.getValue();

        PropertyMetadata prop =
            introspectProperty(
                propName,
                propSchema,
                requiredProperties.contains(propName));
        properties.add(prop);
      }
    }

    String description = schema.getDescription();
    String type = schema.getType() != null ? schema.getType() : "object";

    return new EntitySchemaMetadata(name, type, description, properties, requiredProperties);
  }

  /**
   * Introspect a single property.
   */
  private PropertyMetadata introspectProperty(
      String name, Schema schema, boolean required) {

    String type = schema.getType() != null ? schema.getType() : "string";
    String format = schema.getFormat();
    String description = schema.getDescription();
    Object defaultValue = schema.getDefault();

    // Check for references (relationships)
    String referenceType = null;
    if (schema.get$ref() != null) {
      // Extract entity name from reference: "#/components/schemas/Customer" → "Customer"
      String ref = schema.get$ref();
      referenceType = ref.substring(ref.lastIndexOf('/') + 1);
    }

    return new PropertyMetadata(
        name, type, format, description, required, defaultValue, referenceType);
  }

  /**
   * Extract API endpoints for a given entity (heuristic).
   *
   * @param entityName Entity name to find endpoints for
   * @param openAPI OpenAPI specification
   * @return List of API endpoints (paths) related to the entity
   */
  public List<EndpointMetadata> extractEndpointsForEntity(
      String entityName, OpenAPI openAPI) {

    List<EndpointMetadata> endpoints = new ArrayList<>();

    if (openAPI.getPaths() == null) {
      return endpoints;
    }

    for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
      String path = pathEntry.getKey();  // e.g., "/customers/{id}"
      PathItem item = pathEntry.getValue();

      // Simple heuristic: if path contains entity name (lowercased),
      // it's related to this entity
      String pathLower = path.toLowerCase();
      String entityLower = entityName.toLowerCase();

      if (pathLower.contains(entityLower)) {
        if (item.getGet() != null) {
          endpoints.add(
              new EndpointMetadata(
                  "GET", path, item.getGet().getDescription(), entityName));
        }
        if (item.getPost() != null) {
          endpoints.add(
              new EndpointMetadata(
                  "POST", path, item.getPost().getDescription(), entityName));
        }
        if (item.getPut() != null) {
          endpoints.add(
              new EndpointMetadata(
                  "PUT", path, item.getPut().getDescription(), entityName));
        }
        if (item.getDelete() != null) {
          endpoints.add(
              new EndpointMetadata(
                  "DELETE", path, item.getDelete().getDescription(), entityName));
        }
      }
    }

    return endpoints;
  }

  /** OpenAPI spec metadata. */
  public static class OpenApiSpecMetadata {
    private final OpenAPI openAPI;
    private final List<EntitySchemaMetadata> entitySchemas;

    public OpenApiSpecMetadata(OpenAPI openAPI, List<EntitySchemaMetadata> entitySchemas) {
      this.openAPI = openAPI;
      this.entitySchemas = List.copyOf(entitySchemas);
    }

    public OpenAPI getOpenAPI() {
      return openAPI;
    }

    public List<EntitySchemaMetadata> getEntitySchemas() {
      return entitySchemas;
    }

    public String getTitle() {
      return openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown";
    }

    public String getVersion() {
      return openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "Unknown";
    }
  }

  /** Entity schema metadata. */
  public static class EntitySchemaMetadata {
    private final String name;
    private final String type;
    private final String description;
    private final List<PropertyMetadata> properties;
    private final Set<String> requiredProperties;

    public EntitySchemaMetadata(
        String name,
        String type,
        String description,
        List<PropertyMetadata> properties,
        Set<String> requiredProperties) {
      this.name = name;
      this.type = type;
      this.description = description;
      this.properties = List.copyOf(properties);
      this.requiredProperties = Set.copyOf(requiredProperties);
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public List<PropertyMetadata> getProperties() {
      return properties;
    }

    public Set<String> getRequiredProperties() {
      return requiredProperties;
    }
  }

  /** Property metadata. */
  public static class PropertyMetadata {
    private final String name;
    private final String type;
    private final String format;
    private final String description;
    private final boolean required;
    private final Object defaultValue;
    private final String referenceType;

    public PropertyMetadata(
        String name,
        String type,
        String format,
        String description,
        boolean required,
        Object defaultValue,
        String referenceType) {
      this.name = name;
      this.type = type;
      this.format = format;
      this.description = description;
      this.required = required;
      this.defaultValue = defaultValue;
      this.referenceType = referenceType;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String getFormat() {
      return format;
    }

    public String getDescription() {
      return description;
    }

    public boolean isRequired() {
      return required;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public String getReferenceType() {
      return referenceType;
    }

    public boolean isEntityReference() {
      return referenceType != null;
    }
  }

  /** API endpoint metadata. */
  public static class EndpointMetadata {
    private final String method;
    private final String path;
    private final String description;
    private final String entityName;

    public EndpointMetadata(
        String method, String path, String description, String entityName) {
      this.method = method;
      this.path = path;
      this.description = description;
      this.entityName = entityName;
    }

    public String getMethod() {
      return method;
    }

    public String getPath() {
      return path;
    }

    public String getDescription() {
      return description;
    }

    public String getEntityName() {
      return entityName;
    }
  }
}
