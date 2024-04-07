package systems.symbol.apis;

import systems.symbol.agent.apis.APIException;
import systems.symbol.agent.apis.APIs;
import systems.symbol.agent.apis.I_API;
import systems.symbol.agent.apis.MockAPI;
import systems.symbol.secrets.SecretsException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class APIsTest {
    String HELLO_WORLD = "{\"hello\": \"world\"}";

    @Test
    void mockAPI() throws SecretsException, APIException, IOException {
        APIs apis = new APIs() {
            @Override
            public I_API<String> get(String url) throws SecretsException {
                String secret = findSecret(url);
                return new MockAPI(url, secret, HELLO_WORLD);
            }
        };
        apis.setSecret("IQ_DEV", "hello");
        apis.setSecret("IQ_DEV2", "world");

        apis.withAgent("agent-000")
            .withPermission("urn:systems.symbol/v0/health", "IQ_DEV")
            .withPermission("urn:systems.symbol/v0/health/service", "IQ_DEV2");

        I_API<String> i_api_0 = apis.get("urn:systems.symbol/v0/health/hello");
        assert null != i_api_0;
        assert i_api_0 instanceof MockAPI;
        String authToken = i_api_0.getAuthToken();
        System.out.println("mock.api.0.match: "+i_api_0+", secret: "+ authToken);
        assert null != i_api_0;
        assert null != authToken;
        assert authToken.equals("hello");

        String i_api_0_resp = i_api_0.get(null);
        System.out.println("mock.api.0.body:"+i_api_0_resp);
        assert i_api_0_resp.startsWith("{") && i_api_0_resp.endsWith("}");
//        assert null != i_api_0_resp.header("Authorization");
//        assert i_api_0_resp.header("Authorization").substring(7).equals(authToken);

        I_API i_api_1 = apis.get("urn:systems.symbol/v0/health/service/world");
        System.out.println("mock.api.1: "+i_api_1.getAuthToken());
        assert null != i_api_1;
        assert null != i_api_1.getAuthToken();
        assert i_api_1.getAuthToken().equals("world");

        MockAPI i_api_2 = (MockAPI) apis.get("urn:systems.symbol/v0/missing/");
        System.out.println("mock.api.2: "+i_api_2);
        assert null != i_api_2;
        assert null == i_api_2.secret;
    }

}