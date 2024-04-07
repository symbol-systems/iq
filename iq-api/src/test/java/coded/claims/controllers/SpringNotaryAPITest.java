package systems.symbol.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.annotations.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SpringNotaryAPITest {
static String CE_ID = "12345";
static String CE_TYPE = "systems.symbol";
static String CE_SOURCE = "systems.symbol/notary/test";
static String BODY = "{\"systems.symbol\":\"Hello World\"}";

@Autowired WebTestClient rest;
@Test
void echoWithCorrectHeaders() {
rest.post().uri("/notary")
.header("ce-id", CE_ID)
.header("ce-specversion", "1.0")
.header("ce-type", CE_TYPE)
.header("ce-source", CE_SOURCE)
.contentType(MediaType.APPLICATION_JSON)
.bodyValue(BODY)
.exchange()
.expectStatus().isOk()
.expectHeader().exists("ce-id")
.expectHeader().exists("ce-source")
.expectHeader().exists("ce-type")
.expectHeader().value("ce-id", value -> {
if (!value.equals(CE_ID))
throw new IllegalStateException();
})
.expectHeader().valueEquals("ce-type", CE_TYPE)
.expectHeader().valueEquals("ce-source", CE_SOURCE)
.expectBody(String.class).isEqualTo(BODY);
}
}
