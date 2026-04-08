package systems.symbol.connect.openapi.schema;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import java.util.List;
import java.util.Objects;

/**
 * Generates JSON-LD and semantic RDF triples from introspected OpenAPI schemas.
 *
 * <p>
 * Single Responsibility: Convert EntitySchemaMetadata + PropertyMetadata into RDF triples
 * that represent entity schemas and their semantic meaning.
 * </p>
 *
 * <p>
 * Output format uses a simple JSON-LD-inspired ontology:
 * </p>
 * <ul>
 *   <li>Entity maps represented as instances of `jld:EntityMapping`</li>
 *   <li>Properties linked via `jld:hasProperty`</li>
 *   <li>Type hints via `rdf:type` and `xsd:*` datatypes</li>
 *   <li>References between entities captured as entity links</li>
 * </ul>
 */
public class OpenApiJsonLdMapper {

  private static final String JSONLD_NS = "urn:iq:jsonld:";
  private static final String ENTITY_NS = "urn:iq:entities:";
  private final String apiNamespace;
  private final String entityBaseUri;

  public OpenApiJsonLdMapper(String apiNamespace, String entityBaseUri) {
    this.apiNamespace =
        Objects.requireNonNull(apiNamespace, "apiNamespace must not be null");
    this.entityBaseUri =
        Objects.requireNonNull(entityBaseUri, "entityBaseUri must not be null");
  }

  /**
   * Generate JSON-LD/RDF triples from entity schemas.
   *
   * @param entitySchemas Introspected entity schemas
   * @return RDF Model containing semantic triples
   */
  public Model generateJsonLdMappings(
      List<OpenApiSchemaIntrospector.EntitySchemaMetadata> entitySchemas) {

    Model model = new LinkedHashModel();

    // Register namespaces
    model.setNamespace("jld", JSONLD_NS);
    model.setNamespace("entity", ENTITY_NS);
    model.setNamespace("rdf", RDF.NAMESPACE);
    model.setNamespace("rdfs", RDFS.NAMESPACE);
    model.setNamespace("xsd", XSD.NAMESPACE);

    // Generate triples for each entity
    for (OpenApiSchemaIntrospector.EntitySchemaMetadata entity : entitySchemas) {
      generateEntityMapping(model, entity);
    }

    return model;
  }

  /**
   * Generate RDF triples for a single entity schema.
   *
   * <p>
   * Creates:
   * </p>
   * <ul>
   *   <li>Entity mapping instance</li>
   *   <li>Entity type definition</li>
   *   <li>Property definitions</li>
   *   <li>Reference links to other entities</li>
   * </ul>
   */
  private void generateEntityMapping(
      Model model, OpenApiSchemaIntrospector.EntitySchemaMetadata entity) {

    // 1. Create entity mapping instance
    IRI mappingIri = rdf4j().createIRI(apiNamespace + entity.getName() + "Mapping");
    IRI mappingType = rdf4j().createIRI(JSONLD_NS + "EntityMapping");

    model.add(mappingIri, RDF.TYPE, mappingType);
    model.add(
        mappingIri,
        RDFS.LABEL,
        rdf4j()
            .createLiteral("API Entity: " + entity.getName()));

    if (entity.getDescription() != null) {
      model.add(
          mappingIri,
          RDFS.COMMENT,
          rdf4j()
              .createLiteral(entity.getDescription()));
    }

    // 2. Link to entity type definition
    IRI entityTypeIri = rdf4j().createIRI(ENTITY_NS + entity.getName());
    IRI entityTypeProp = rdf4j().createIRI(JSONLD_NS + "entity");
    model.add(mappingIri, entityTypeProp, entityTypeIri);

    model.add(entityTypeIri, RDF.TYPE, rdf4j().createIRI(JSONLD_NS + "EntityType"));
    model.add(
        entityTypeIri,
        RDFS.LABEL,
        rdf4j()
            .createLiteral(entity.getName()));

    // 3. Create property definitions
    IRI hasPropsProp = rdf4j().createIRI(JSONLD_NS + "hasProperty");

    for (OpenApiSchemaIntrospector.PropertyMetadata prop : entity.getProperties()) {
      IRI propIri = rdf4j().createIRI(apiNamespace + entity.getName() + ":" + prop.getName());

      // Link from entity to property
      model.add(mappingIri, hasPropsProp, propIri);

      // Property instance
      IRI propType = rdf4j().createIRI(JSONLD_NS + "Property");
      model.add(propIri, RDF.TYPE, propType);

      model.add(
          propIri,
          RDFS.LABEL,
          rdf4j()
              .createLiteral(prop.getName()));

      if (prop.getDescription() != null) {
        model.add(
            propIri,
            RDFS.COMMENT,
            rdf4j()
                .createLiteral(prop.getDescription()));
      }

      // Property attributes
      IRI typeProp = rdf4j().createIRI(JSONLD_NS + "type");
      model.add(propIri, typeProp, rdf4j().createLiteral(prop.getType()));

      if (prop.getFormat() != null) {
        IRI formatProp = rdf4j().createIRI(JSONLD_NS + "format");
        model.add(propIri, formatProp, rdf4j().createLiteral(prop.getFormat()));
      }

      // Required flag
      IRI requiredProp = rdf4j().createIRI(JSONLD_NS + "required");
      model.add(propIri, requiredProp, rdf4j().createLiteral(prop.isRequired()));

      // Datatype mapping (OpenAPI type → XSD type)
      IRI xsdType = mapOpenApiTypeToXSD(prop.getType());
      IRI datatypeProp = rdf4j().createIRI(JSONLD_NS + "datatype");
      model.add(propIri, datatypeProp, xsdType);

      // If this property references another entity, create a link
      if (prop.isEntityReference()) {
        IRI referenceProp = rdf4j().createIRI(JSONLD_NS + "reference");
        IRI referencedEntity = rdf4j().createIRI(ENTITY_NS + prop.getReferenceType());
        model.add(propIri, referenceProp, referencedEntity);
      }
    }

    // 4. Mark required properties
    IRI requiredSetProp = rdf4j().createIRI(JSONLD_NS + "requiredProperties");
    for (String reqProp : entity.getRequiredProperties()) {
      model.add(
          mappingIri,
          requiredSetProp,
          rdf4j()
              .createLiteral(reqProp));
    }
  }

  /** Map OpenAPI type string to XSD datatype IRI. */
  private IRI mapOpenApiTypeToXSD(String openApiType) {
    return switch (openApiType.toLowerCase()) {
      case "integer" -> XSD.INTEGER;
      case "number" -> XSD.DECIMAL;
      case "string" -> XSD.STRING;
      case "boolean" -> XSD.BOOLEAN;
      case "array" -> rdf4j().createIRI(RDF.NAMESPACE + "Seq");
      case "object" -> rdf4j().createIRI("urn:iq:types:Object");
      default -> XSD.STRING;
    };
  }

  /** RDF4J value factory shorthand. */
  private static org.eclipse.rdf4j.model.ValueFactory rdf4j() {
    return org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
  }
}
