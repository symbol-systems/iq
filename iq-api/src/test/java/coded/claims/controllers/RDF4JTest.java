package systems.symbol.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.annotations.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RDF4JTest {

    @Autowired
    private WebTestClient rest;

    @Test
    void echoWithCorrectHeaders() {

//        rest.post().uri("/rdf/test").header("ce-id", "12345") //
//                .header("ce-specversion", "1.0") //
//                .header("ce-type", "io.spring.event") //
//                .header("ce-source", "https://spring.io/events") //
//                .contentType(MediaType.APPLICATION_JSON) //
//                .bodyValue("{\"value\":\"Hello World\"}") //
//                .exchange() //
//                .expectStatus().isOk() //
//                .expectHeader().exists("ce-id") //
//                .expectHeader().exists("ce-source") //
//                .expectHeader().exists("ce-type") //
//                .expectHeader().value("ce-id", value -> {
//                    if (value.equals("12345"))
//                        throw new IllegalStateException();
//                }) //
//                .expectHeader().valueEquals("ce-type", "io.spring.event.Foo") //
//                .expectHeader().valueEquals("ce-source", "https://spring.io/foos") //
//                .expectBody(String.class).isEqualTo("{\"value\":\"Dave\"}");

    }
}
