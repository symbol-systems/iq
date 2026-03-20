package systems.symbol.controller.ux;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.io.IOCopier;
import systems.symbol.secrets.SecretsException;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ModelAPIIT {

private static String VALID_AUTH_HEADER;
private static final String INVALID_AUTH_HEADER = "Bearer invalid.token";
private static final String REALM = "iq";
private static final String QUERY = "ai/iq";

@BeforeEach
public void setup() throws IOException {
File jwtFile = new File("./.iq/jwt/" + REALM + ".jwt");
if (jwtFile.exists()) {
VALID_AUTH_HEADER = "Bearer " + IOCopier.load(jwtFile);
System.out.println("test.setup.jwt: " + VALID_AUTH_HEADER);
}
}

@Test
public void testGraphValidRequest() throws IOException, SecretsException {
if (VALID_AUTH_HEADER == null)
return;

given()
.header("Authorization", VALID_AUTH_HEADER)
.pathParam("realm", REALM)
.pathParam("query", QUERY)
.when()
.get("/ux/model/{realm}/{query}")
.then()
.statusCode(200)
.contentType(ContentType.JSON);
}

@Test
public void testGraphUnauthorized() {
if (VALID_AUTH_HEADER == null)
return;
given()
.header("Authorization", INVALID_AUTH_HEADER)
.pathParam("realm", REALM)
.pathParam("query", QUERY)
.when()
.get("/ux/model/{realm}/{query}")
.then()
.statusCode(403);
}

@Test
public void testGraphRealmNotFound() {
if (VALID_AUTH_HEADER == null)
return;
given()
.header("Authorization", VALID_AUTH_HEADER)
.pathParam("realm", "oops")
.pathParam("query", QUERY)
.when()
.get("/ux/model/{realm}/{query}")
.then()
.statusCode(404);
}

@Test
public void testGraphQueryMissing() {
if (VALID_AUTH_HEADER == null)
return;
given()
.header("Authorization", VALID_AUTH_HEADER)
.pathParam("realm", REALM)
.pathParam("query", "oops")
.when()
.get("/ux/model/{realm}/{query}")
.then()
.statusCode(404);
}
}
