package systems.symbol.connect.jdbc.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for R2RML generation from JDBC schema metadata.
 *
 * Tests that introspected tables are correctly converted to W3C R2RML triples.
 */
@DisplayName("JdbcR2RMLGenerator")
class JdbcR2RMLGeneratorTest {

  private Connection h2Connection;
  private JdbcSchemaIntrospector introspector;
  private JdbcR2RMLGenerator generator;

  @BeforeEach
  void setUp() throws Exception {
    // Set up H2 in-memory database
    h2Connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");

    try (Statement stmt = h2Connection.createStatement()) {
      stmt.execute(
          "CREATE TABLE users ("
              + "  id INT PRIMARY KEY,"
              + "  name VARCHAR(100) NOT NULL,"
              + "  email VARCHAR(100),"
              + "  active BOOLEAN DEFAULT TRUE,"
              + "  created_date DATE"
              + ")");

      stmt.execute(
          "CREATE TABLE profiles ("
              + "  id INT PRIMARY KEY,"
              + "  user_id INT NOT NULL UNIQUE,"
              + "  bio VARCHAR(500),"
              + "  FOREIGN KEY (user_id) REFERENCES users(id)"
              + ")");
    }

    introspector = new JdbcSchemaIntrospector(h2Connection);
    generator = new JdbcR2RMLGenerator("urn:iq:db:", "urn:iq:entities:");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (h2Connection != null && !h2Connection.isClosed()) {
      h2Connection.close();
    }
  }

  @Test
  @DisplayName("should generate R2RML model")
  void generateR2RMLModel() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    assertNotNull(r2rml);
    assertFalse(r2rml.isEmpty(), "R2RML model should not be empty");
    assertTrue(r2rml.size() > 0);
  }

  @Test
  @DisplayName("should create TriplesMap for each table")
  void createTriplesMapsForTables() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have TriplesMap instances
    long triplesMaps = r2rml.filter(
            null,
            RDF.TYPE,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#TriplesMap"))
        .size();

    assertEquals(2, triplesMaps, "Should have 2 TriplesMap instances (one per table)");
  }

  @Test
  @DisplayName("should include table names in R2RML")
  void includeTableNames() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have tableName literals
    long tableNames = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#tableName"),
            null)
        .size();

    assertEquals(2, tableNames, "Should have 2 tableName properties");
  }

  @Test
  @DisplayName("should include column properties in R2RML")
  void includeColumnProperties() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have column references
    long columns = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#column"),
            null)
        .size();

    assertTrue(
        columns > 0,
        "Should have column properties in predicateObjectMap");
  }

  @Test
  @DisplayName("should map SQL types to XSD types")
  void mapSqlTypesToXsd() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have XSD datatype properties
    long xsdTypes = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#datatype"),
            null)
        .size();

    assertTrue(xsdTypes > 0, "Should have XSD datatype mappings");
  }

  @Test
  @DisplayName("should include URI templates for subjects")
  void includeUriTemplates() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have template properties with URI patterns
    long templates = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#template"),
            null)
        .size();

    assertEquals(2, templates, "Should have URI template for each table");
  }

  @Test
  @DisplayName("should include class definitions for entities")
  void includeClassDefinitions() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have class properties (r2rml:class)
    long classProps = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#class"),
            null)
        .size();

    assertEquals(2, classProps, "Should have class property for each entity");
  }

  @Test
  @DisplayName("should handle foreign key relationships")
  void handleForeignKeyRelationships() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have parentTriplesMap references
    long fkRefs = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#parentTriplesMap"),
            null)
        .size();

    assertTrue(fkRefs > 0, "Should have foreign key mappings");
  }

  @Test
  @DisplayName("should include join conditions for FKs")
  void includeJoinConditions() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Should have joinCondition property
    long joins = r2rml.filter(
            null,
            org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
                .createIRI("http://www.w3.org/ns/r2rml#joinCondition"),
            null)
        .size();

    assertTrue(joins > 0, "Should have joinCondition for foreign keys");
  }

  @Test
  @DisplayName("should produce valid RDF triples")
  void produceValidRdf() throws Exception {
    List<JdbcSchemaIntrospector.TableMetadata> tables =
        introspector.introspectTables();

    Model r2rml = generator.generateR2RML(tables);

    // Verify model integrity
    assertTrue(r2rml.subjects().iterator().hasNext(), "Should have subjects");
    assertTrue(r2rml.predicates().iterator().hasNext(), "Should have predicates");
    assertTrue(r2rml.objects().iterator().hasNext(), "Should have objects");
  }
}
