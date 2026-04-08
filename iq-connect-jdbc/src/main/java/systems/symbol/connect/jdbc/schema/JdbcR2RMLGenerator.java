package systems.symbol.connect.jdbc.schema;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Generates W3C R2RML triples from introspected JDBC schema.
 *
 * <p>
 * Single Responsibility: Convert TableMetadata + ColumnMetadata into RDF triples
 * that represent R2RML mappings. This follows the W3C R2RML standard.
 * </p>
 *
 * <p>
 * R2RML defines how to map SQL tables to RDF:
 * </p>
 * <ul>
 *   <li>TriplesMap: defines a mapping for one table</li>
 *   <li>logicalTable: the source SQL table</li>
 *   <li>subjectMap: generates subject URIs (templates, typically keyed by PK)</li>
 *   <li>predicateObjectMap: maps columns to RDF properties</li>
 * </ul>
 *
 * @see <a href="https://www.w3.org/TR/r2rml/">W3C R2RML Specification</a>
 */
public class JdbcR2RMLGenerator {

  /** Base namespace for generated R2RML vocabularies. */
  private static final String R2RML_NS = "http://www.w3.org/ns/r2rml#";
  /** Base namespace for entity type definitions. */
  private final String entityBaseUri;
  /** Namespace (URI prefix) for generated entities. */
  private final String entityNamespace;

  public JdbcR2RMLGenerator(String entityBaseUri, String entityNamespace) {
    this.entityBaseUri = Objects.requireNonNull(entityBaseUri, "entityBaseUri must not be null");
    this.entityNamespace =
        Objects.requireNonNull(entityNamespace, "entityNamespace must not be null");
  }

  /**
   * Generate R2RML triples for a list of tables.
   *
   * @param tables Introspected table metadata
   * @return RDF Model containing R2RML triples
   */
  public Model generateR2RML(List<JdbcSchemaIntrospector.TableMetadata> tables) {
    Model model = new LinkedHashModel();

    // Register common namespaces
    model.setNamespace("r2rml", R2RML_NS);
    model.setNamespace("rdf", RDF.NAMESPACE);
    model.setNamespace("rdfs", RDFS.NAMESPACE);
    model.setNamespace("xsd", XSD.NAMESPACE);
    model.setNamespace("ex", entityNamespace);

    // Generate R2RML mapping for each table
    for (JdbcSchemaIntrospector.TableMetadata table : tables) {
      generateTableMapping(model, table);
    }

    return model;
  }

  /**
   * Generate R2RML triples for a single table.
   *
   * <p>
   * Creates a TriplesMap instance with:
   * </p>
   * <ul>
   *   <li>logicalTable: table name</li>
   *   <li>subjectMap: template with primary key</li>
   *   <li>predicateObjectMap: one entry per column</li>
   * </ul>
   */
  private void generateTableMapping(
      Model model, JdbcSchemaIntrospector.TableMetadata table) {

    // Create unique IRI for this mapping
    String mappingId = sanitizeName(table.getTableName()) + "Mapping";
    IRI mappingIri = rdf4j().createIRI(entityNamespace + mappingId);

    // 1. Create TriplesMap instance
    IRI triplesMapClass = createIRI(R2RML_NS + "TriplesMap");
    model.add(mappingIri, RDF.TYPE, triplesMapClass);
    model.add(mappingIri, RDFS.LABEL, rdf4j()
        .createLiteral("R2RML Mapping: " + table.getTableName()));

    // 2. Set logicalTable (the SQL source)
    IRI logicalTableProp = createIRI(R2RML_NS + "logicalTable");
    IRI logicalTableNode = rdf4j().createBNode();
    model.add(mappingIri, logicalTableProp, logicalTableNode);

    IRI tableNameProp = createIRI(R2RML_NS + "tableName");
    model.add(
        logicalTableNode,
        tableNameProp,
        rdf4j().createLiteral(table.getTableName()));

    // 3. Create subjectMap (URI template using primary keys)
    IRI subjectMapProp = createIRI(R2RML_NS + "subjectMap");
    IRI subjectMapNode = rdf4j().createBNode();
    model.add(mappingIri, subjectMapProp, subjectMapNode);

    // Build template: urn:iq:table:tablename:column1:column2
    String uriTemplate = buildUriTemplate(table);
    IRI templateProp = createIRI(R2RML_NS + "template");
    model.add(subjectMapNode, templateProp, rdf4j().createLiteral(uriTemplate));

    // Add class to subjects
    IRI classProp = createIRI(R2RML_NS + "class");
    String entityType = sanitizeName(table.getTableName());
    IRI entityClass = rdf4j().createIRI(entityNamespace + entityType);
    model.add(subjectMapNode, classProp, entityClass);

    // 4. Create predicateObjectMaps for each column
    for (JdbcSchemaIntrospector.ColumnMetadata col : table.getColumns()) {
      if (col.isPrimaryKey()) {
        continue; // Skip PK columns in mappings (they're in template)
      }

      IRI podmNode = rdf4j().createBNode();
      model.add(mappingIri, createIRI(R2RML_NS + "predicateObjectMap"), podmNode);

      // Predicate: entity namespace + column name
      IRI predicate = rdf4j().createIRI(entityNamespace + col.getColumnName());
      model.add(podmNode, createIRI(R2RML_NS + "predicate"), predicate);

      // ObjectMap: column reference
      IRI objectMapNode = rdf4j().createBNode();
      model.add(podmNode, createIRI(R2RML_NS + "objectMap"), objectMapNode);

      IRI columnProp = createIRI(R2RML_NS + "column");
      model.add(objectMapNode, columnProp, rdf4j().createLiteral(col.getColumnName()));

      // Datatype hint (SQL type → XSD type)
      IRI xsdType = mapSQLTypeToXSD(col.getDataType());
      IRI datatypeProp = createIRI(R2RML_NS + "datatype");
      model.add(objectMapNode, datatypeProp, xsdType);
    }

    // 5. Add foreign key references (JoinConditions)
    for (JdbcSchemaIntrospector.ForeignKeyMetadata fk : table.getForeignKeys()) {
      generateForeignKeyMapping(model, mappingIri, table, fk);
    }
  }

  /**
   * Generate predicateObjectMap for a foreign key reference.
   *
   * <p>
   * Creates a reference to another table's mapping.
   * </p>
   */
  private void generateForeignKeyMapping(
      Model model,
      IRI parentMappingIri,
      JdbcSchemaIntrospector.TableMetadata parentTable,
      JdbcSchemaIntrospector.ForeignKeyMetadata fk) {

    IRI podmNode = rdf4j().createBNode();
    model.add(
        parentMappingIri,
        createIRI(R2RML_NS + "predicateObjectMap"),
        podmNode);

    // Predicate: reference to parent table (readable name from FK column)
    IRI predicate =
        rdf4j()
            .createIRI(entityNamespace + sanitizeName(fk.getReferencedTable()));
    model.add(podmNode, createIRI(R2RML_NS + "predicate"), predicate);

    // ObjectMap with JoinCondition
    IRI objectMapNode = rdf4j().createBNode();
    model.add(podmNode, createIRI(R2RML_NS + "objectMap"), objectMapNode);

    // Reference the parent table's mapping
    String referencedMappingId = sanitizeName(fk.getReferencedTable()) + "Mapping";
    IRI referencedMapping = rdf4j().createIRI(entityNamespace + referencedMappingId);

    IRI parentMapProp = createIRI(R2RML_NS + "parentTriplesMap");
    model.add(objectMapNode, parentMapProp, referencedMapping);

    // Join condition: child column = parent column
    IRI joinCondProp = createIRI(R2RML_NS + "joinCondition");
    IRI joinCondNode = rdf4j().createBNode();
    model.add(objectMapNode, joinCondProp, joinCondNode);

    IRI childProp = createIRI(R2RML_NS + "child");
    IRI parentProp = createIRI(R2RML_NS + "parent");

    model.add(joinCondNode, childProp, rdf4j().createLiteral(fk.getFkColumn()));
    model.add(joinCondNode, parentProp, rdf4j().createLiteral(fk.getReferencedColumn()));
  }

  /**
   * Build URI template for subject. Format: urn:iq:table:{tablename}:{pk1}:{pk2}...
   */
  private String buildUriTemplate(JdbcSchemaIntrospector.TableMetadata table) {
    StringBuilder sb = new StringBuilder("urn:iq:table:");
    sb.append(sanitizeName(table.getTableName()));

    for (JdbcSchemaIntrospector.ColumnMetadata col : table.getColumns()) {
      if (col.isPrimaryKey()) {
        sb.append(":{").append(col.getColumnName()).append("}");
      }
    }

    return sb.toString();
  }

  /** Map SQL type code to XSD datatype. */
  private IRI mapSQLTypeToXSD(int sqlType) {
    return switch (sqlType) {
      case Types.BIGINT, Types.NUMERIC, Types.DECIMAL -> XSD.LONG;
      case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> XSD.INT;
      case Types.FLOAT, Types.REAL, Types.DOUBLE -> XSD.DOUBLE;
      case Types.BOOLEAN, Types.BIT -> XSD.BOOLEAN;
      case Types.DATE -> XSD.DATE;
      case Types.TIME, Types.TIME_WITH_TIMEZONE -> XSD.TIME;
      case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> XSD.DATETIME;
      case Types.CLOB, Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> XSD.STRING;
      case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> XSD.HEXBINARY;
      default -> XSD.STRING;
    };
  }

  /** Sanitize string for use as part of IRI/namespace. */
  private String sanitizeName(String name) {
    return name.replaceAll("[^a-zA-Z0-9_]", "_");
  }

  /** Shorthand for creating IRIs. */
  private IRI createIRI(String iri) {
    return rdf4j().createIRI(iri);
  }

  /** Access to RDF4J factory (would be injected in production). */
  private static org.eclipse.rdf4j.model.ValueFactory rdf4j() {
    return org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
  }
}
