package systems.symbol.controller.platform;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class RuntimeAPIT {

    @Test
    public void testRuntimeHealthEndpoint() {
        given()
                .when().get("/runtime/api/health")
                .then()
                .statusCode(200)
                .body("healthy", equalTo(true));
    }

    @Test
    public void testRuntimeStartAndDumpEndpoint() {
        given().when().post("/runtime/api/start").then().statusCode(200);
        given().when().post("/runtime/api/dump?path=./tmp/test-dump.tar.gz").then().statusCode(200);
        given().when().post("/runtime/api/stop").then().statusCode(200);
    }
}
