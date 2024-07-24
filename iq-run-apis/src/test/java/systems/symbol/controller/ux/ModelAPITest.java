package systems.symbol.controller.ux;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.io.IOCopier;
import systems.symbol.secrets.SecretsException;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ModelAPITest {

    private static String VALID_AUTH_HEADER;
    private static final String INVALID_AUTH_HEADER = "Bearer invalid.token";
    private static final String REALM = "QRX";
    private static final String QUERY = "self/aware";

    @BeforeEach
    public void setup() throws IOException {
        VALID_AUTH_HEADER = "Bearer "+IOCopier.load(new File("./.iq/vault/jwt/" + REALM + ".jwt"));
        System.out.println("test.setup.jwt: "+VALID_AUTH_HEADER);
    }

    @Test
    public void testGraphValidRequest() throws IOException, SecretsException {
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
        given()
                .header("Authorization", VALID_AUTH_HEADER)
                .pathParam("realm", "nonExistentRealm")
                .pathParam("query", QUERY)
                .when()
                .get("/ux/model/{realm}/{query}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGraphQueryMissing() {
        given()
                .header("Authorization", VALID_AUTH_HEADER)
                .pathParam("realm", REALM)
                .pathParam("query", "")
                .when()
                .get("/ux/model/{realm}/{query}")
                .then()
                .statusCode(404);
    }
}
