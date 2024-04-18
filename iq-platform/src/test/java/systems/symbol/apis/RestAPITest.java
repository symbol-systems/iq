package systems.symbol.apis;

import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.UnknownHostException;

class RestAPITest {

    @Test
    void testSimpleAPIGet() throws IOException, APIException {
        try {
            RestAPI api = new RestAPI("https://mocki.io/v1/d4867d8b-b5d5-4a48-a4ab-79131b5809b8", null);

            Response response = api.get();
            assert null != response;
            assert null != response.body();
            String body = response.body().string();
            assert body.contains("{") && body.contains("}");

        } catch (UnknownHostException e) {
            System.err.println("mock host offline: "+e.getMessage());
        }
    }
}