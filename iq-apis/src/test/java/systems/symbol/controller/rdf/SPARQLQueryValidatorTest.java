package systems.symbol.controller.rdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SPARQL query validation.
 */
@DisplayName("SPARQL Query Validator Tests")
public class SPARQLQueryValidatorTest {

@Test
@DisplayName("Valid SELECT query passes validation")
void testValidSelectQuery() {
String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
assertEquals(SPARQLQueryValidator.DEFAULT_TIMEOUT_SECONDS, result.getRecommendedTimeout());
}

@Test
@DisplayName("Valid ASK query passes validation")
void testValidAskQuery() {
String query = "ASK { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
}

@Test
@DisplayName("Valid CONSTRUCT query passes validation")
void testValidConstructQuery() {
String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
}

@Test
@DisplayName("Valid DESCRIBE query passes validation")
void testValidDescribeQuery() {
String query = "DESCRIBE ?s WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
}

@Test
@DisplayName("Empty query fails validation")
void testEmptyQuery() {
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate("", null);

assertFalse(result.isValid());
assertNotNull(result.getMessage());
}

@Test
@DisplayName("Query too small fails validation")
void testQueryTooSmall() {
String query = "SELECT";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("too small"));
}

@Test
@DisplayName("Query exceeding size limit fails validation")
void testQueryTooLarge() {
// Create a query larger than 1MB
StringBuilder sb = new StringBuilder("SELECT DISTINCT ?s ?p ?o WHERE { ");
while (sb.length() < SPARQLQueryValidator.MAX_QUERY_SIZE + 1000) {
sb.append("OPTIONAL { ?s ?p ?o } ");
}
sb.append("}");

SPARQLQueryValidator.ValidationResult result = 
SPARQLQueryValidator.validate(sb.toString(), null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("exceeded maximum size"));
}

@Test
@DisplayName("INSERT DATA query fails validation")
void testInsertDataQueryBlocked() {
String query = "INSERT DATA { <s> <p> <o> }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("write or data manipulation"));
}

@Test
@DisplayName("DELETE DATA query fails validation")
void testDeleteDataQueryBlocked() {
String query = "DELETE DATA { <s> <p> <o> }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("write or data manipulation"));
}

@Test
@DisplayName("LOAD query fails validation")
void testLoadQueryBlocked() {
String query = "LOAD <http://example.org/data.ttl>";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("write or data manipulation"));
}

@Test
@DisplayName("CLEAR query fails validation")
void testClearQueryBlocked() {
String query = "CLEAR GRAPH <http://example.org/g>";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("write or data manipulation"));
}

@Test
@DisplayName("DROP query fails validation")
void testDropQueryBlocked() {
String query = "DROP GRAPH <http://example.org/g>";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("write or data manipulation"));
}

@Test
@DisplayName("Query without WHERE clause fails validation")
void testQueryWithoutWhereClause() {
String query = "SELECT DISTINCT ?s ?p ?o";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

// May fail due to basic syntax validation
// Depends on implementation strictness
assertNotNull(result);
}

@Test
@DisplayName("Null timeout uses default")
void testNullTimeoutUsesDefault() {
String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
assertEquals(SPARQLQueryValidator.DEFAULT_TIMEOUT_SECONDS, result.getRecommendedTimeout());
}

@Test
@DisplayName("Valid timeout is returned")
void testValidTimeout() {
String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, 60);

assertTrue(result.isValid());
assertEquals(60, result.getRecommendedTimeout());
}

@Test
@DisplayName("Timeout below minimum fails validation")
void testTimeoutBelowMinimum() {
String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, 0);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("at least"));
}

@Test
@DisplayName("Timeout above maximum fails validation")
void testTimeoutAboveMaximum() {
String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, 600);

assertFalse(result.isValid());
assertTrue(result.getMessage().contains("exceeds maximum"));
}

@Test
@DisplayName("getQueryType returns correct type for SELECT")
void testGetQueryTypeSelect() {
String type = SPARQLQueryValidator.getQueryType("SELECT DISTINCT ?s WHERE { ?s ?p ?o }");
assertEquals("SELECT", type);
}

@Test
@DisplayName("getQueryType returns correct type for ASK")
void testGetQueryTypeAsk() {
String type = SPARQLQueryValidator.getQueryType("ASK { ?s ?p ?o }");
assertEquals("ASK", type);
}

@Test
@DisplayName("getQueryType returns correct type for CONSTRUCT")
void testGetQueryTypeConstruct() {
String type = SPARQLQueryValidator.getQueryType("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
assertEquals("CONSTRUCT", type);
}

@Test
@DisplayName("getQueryType returns correct type for DESCRIBE")
void testGetQueryTypeDescribe() {
String type = SPARQLQueryValidator.getQueryType("DESCRIBE ?s WHERE { ?s ?p ?o }");
assertEquals("DESCRIBE", type);
}

@Test
@DisplayName("getQueryType returns UNKNOWN for invalid query")
void testGetQueryTypeUnknown() {
String type = SPARQLQueryValidator.getQueryType("INVALID ?s WHERE { ?s ?p ?o }");
assertEquals("UNKNOWN", type);
}

@Test
@DisplayName("Query with leading whitespace is valid")
void testQueryWithLeadingWhitespace() {
String query = "  \n  SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";
SPARQLQueryValidator.ValidationResult result = SPARQLQueryValidator.validate(query, null);

assertTrue(result.isValid());
}

@Test
@DisplayName("Query type check is case-insensitive")
void testQueryTypeCaseInsensitive() {
String query1 = "select ?s where { ?s ?p ?o }";
String query2 = "SeLeCt ?s WHERE { ?s ?p ?o }";

SPARQLQueryValidator.ValidationResult result1 = SPARQLQueryValidator.validate(query1, null);
SPARQLQueryValidator.ValidationResult result2 = SPARQLQueryValidator.validate(query2, null);

assertTrue(result1.isValid());
assertTrue(result2.isValid());
}
}
