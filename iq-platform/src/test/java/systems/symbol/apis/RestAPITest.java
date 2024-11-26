package systems.symbol.apis;

import systems.symbol.tools.APIException;
import systems.symbol.tools.RestAPI;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.UnknownHostException;

class RestAPITest {

@Test
void testSimpleAPIGet() throws IOException, APIException {
try {
RestAPI api = new RestAPI("https://mocki.io/v1/d4867d8b-b5d5-4a48-a4ab-79131b5809b8");

Response response = api.get();
assert null != response;
ResponseBody body = response.body();
assert null != body;
String body$ = body.string();
assert body$.contains("{") && body$.contains("}");
response.close();

} catch (UnknownHostException e) {
System.err.println("mock host offline: " + e.getMessage());
}
}
}