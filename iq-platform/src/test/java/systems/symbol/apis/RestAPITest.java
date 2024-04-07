package systems.symbol.apis;

import systems.symbol.agent.apis.APIException;
import systems.symbol.agent.apis.RestAPI;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class RestAPITest {

@Test
void head() {
}

@Test
void get() throws IOException, APIException {
RestAPI api = new RestAPI("https://mocki.io/v1/d4867d8b-b5d5-4a48-a4ab-79131b5809b8", null);

Response response = api.get(null);
assert null != response;
assert null != response.body();
String body = response.body().string();
assert null != body;
assert body.contains("{") && body.contains("}");
//System.out.println("api.rest.json: "+body);
}

@Test
void delete() {
}

@Test
void post() {
}

@Test
void put() {
}
}