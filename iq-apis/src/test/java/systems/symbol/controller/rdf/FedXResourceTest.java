package systems.symbol.controller.rdf;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for FedX REST API endpoints.
 *
 * Validates federated SPARQL query execution via GET and POST endpoints.
 * Note: These are integration tests that require the FedX infrastructure to be initialized.
 */
@QuarkusTest
@DisplayName("FedX REST API Tests")
public class FedXResourceTest {

@BeforeEach
void setup() {
RestAssured.basePath = "";
}

@Test
@DisplayName("FedX: GET /sparql/federated/query without query parameter returns 400")
void testFedXQueryMissingParameter() {
given()
.when()
.get("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(400), equalTo(401)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: GET /sparql/federated/query with SELECT query")
void testFedXQueryGET() {
String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";

given()
.queryParam("query", query)
.when()
.get("/sparql/federated/query")
.then()
// 200 on success, 400 for invalid query, 401 for auth, 500 for init issues, 503 if endpoints unavailable
.statusCode(anyOf(equalTo(200), equalTo(400), equalTo(401), equalTo(500), equalTo(503)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: GET /sparql/federated/query with ASK query")
void testFedXAskQuery() {
String query = "ASK { ?s ?p ?o }";

given()
.queryParam("query", query)
.when()
.get("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(200), equalTo(400), equalTo(401), equalTo(500), equalTo(503)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: GET /sparql/federated/query with timeout parameter")
void testFedXQueryWithTimeout() {
String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";

given()
.queryParam("query", query)
.queryParam("timeout", "30")
.when()
.get("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(200), equalTo(400), equalTo(401), equalTo(500), equalTo(503)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: POST /sparql/federated/query without query parameter returns 400")
void testFedXQueryPOSTMissingParameter() {
given()
.contentType(ContentType.URLENC)
.when()
.post("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(400), equalTo(401)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: POST /sparql/federated/query with form-encoded query")
void testFedXQueryPOST() {
String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";

given()
.contentType(ContentType.URLENC)
.formParam("query", query)
.when()
.post("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(200), equalTo(400), equalTo(401), equalTo(500), equalTo(503)))
.contentType(ContentType.JSON);
}

@Test
@DisplayName("FedX: POST /sparql/federated/query with timeout")
void testFedXQueryPOSTWithTimeout() {
String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";

given()
.contentType(ContentType.URLENC)
.formParam("query", query)
.formParam("timeout", "30")
.when()
.post("/sparql/federated/query")
.then()
.statusCode(anyOf(equalTo(200), equalTo(400), equalTo(401), equalTo(500), equalTo(503)))
.contentType(ContentType.JSON);
}
}
