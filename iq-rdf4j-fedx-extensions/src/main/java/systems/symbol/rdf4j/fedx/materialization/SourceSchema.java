package systems.symbol.rdf4j.fedx.materialization;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object holding cached schema metadata for a source.
 *
 * <p>Represents the structure of a data source (tables/entities and their columns/properties),
 * independent of the actual data. Created by schema introspection, cached by {@code
 * I_SchemaMaterializer}.
 */
public final class SourceSchema {

  private final String sourceId;
  private final List<EntitySchema> entities; // tables/entities discovered from source
  private final long cachedAtMs; // timestamp when this schema was introspected
  private final String sourceType; // "JDBC", "API", "RDF", "SPARQL"

  /**
   * Construct a source schema.
   *
   * @param sourceId source identifier (endpoint nodeId)
   * @param entities list of entity/table schemas
   * @param sourceType source type (e.g., "JDBC", "API")
   */
  public SourceSchema(String sourceId, List<EntitySchema> entities, String sourceType) {
    this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
    this.entities = Objects.requireNonNull(entities, "entities cannot be null");
    this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
    this.cachedAtMs = System.currentTimeMillis();
  }

  /** @return source identifier */
  public String sourceId() {
    return sourceId;
  }

  /** @return list of entity/table schemas */
  public List<EntitySchema> entities() {
    return entities;
  }

  /** @return timestamp when schema was cached (milliseconds since epoch) */
  public long cachedAtMs() {
    return cachedAtMs;
  }

  /** @return source type (JDBC, API, RDF, SPARQL) */
  public String sourceType() {
    return sourceType;
  }

  /**
   * Immutable entity/table schema.
   *
   * <p>Represents one table (SQL) or entity (API/RDF) in a source, with its columns/properties.
   */
  public static final class EntitySchema {
    private final String name; // table name or entity type
    private final String description; // optional description
    private final List<PropertySchema> properties; // columns or properties
    private final Map<String, String> metadata; // additional metadata (e.g., table owner, entity
    // namespace)

    /**
     * Construct an entity schema.
     *
     * @param name entity/table name
     * @param description optional description
     * @param properties list of column/property schemas
     * @param metadata additional metadata
     */
    public EntitySchema(
        String name,
        String description,
        List<PropertySchema> properties,
        Map<String, String> metadata) {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      this.description = description;
      this.properties = Objects.requireNonNull(properties, "properties cannot be null");
      this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
    }

    public String name() {
      return name;
    }

    public String description() {
      return description;
    }

    public List<PropertySchema> properties() {
      return properties;
    }

    public Map<String, String> metadata() {
      return metadata;
    }
  }

  /**
   * Immutable property/column schema.
   *
   * <p>Represents one column (SQL) or property (API/RDF) of an entity.
   */
  public static final class PropertySchema {
    private final String name; // column/property name
    private final String type; // XSD type or database-native type
    private final boolean nullable; // whether value can be NULL/absent
    private final boolean primaryKey; // whether this is a primary key
    private final String description; // optional description

    /**
     * Construct a property schema.
     *
     * @param name property name
     * @param type XSD or database type
     * @param nullable whether nullable
     * @param primaryKey whether primary key
     * @param description optional description
     */
    public PropertySchema(
        String name,
        String type,
        boolean nullable,
        boolean primaryKey,
        String description) {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      this.type = Objects.requireNonNull(type, "type cannot be null");
      this.nullable = nullable;
      this.primaryKey = primaryKey;
      this.description = description;
    }

    public String name() {
      return name;
    }

    public String type() {
      return type;
    }

    public boolean nullable() {
      return nullable;
    }

    public boolean primaryKey() {
      return primaryKey;
    }

    public String description() {
      return description;
    }
  }
}
