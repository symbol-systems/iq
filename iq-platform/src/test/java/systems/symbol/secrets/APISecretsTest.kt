package systems.symbol.secrets

import okhttp3.Response
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.util.Values
import org.junit.jupiter.api.Test
import systems.symbol.agent.tools.APIException
import systems.symbol.agent.tools.I_API
import systems.symbol.agent.tools.MockAPI
import systems.symbol.COMMONS
import java.io.IOException

internal class APISecretsTest {
    var HELLO_WORLD: String = "{\"hello\": \"world\"}"
    var BASE_URL: String = "http://localhost:8080/"

    var self: IRI = Values.iri(COMMONS.IQ_NS_TEST)

    @Test
    @Throws(SecretsException::class, APIException::class, IOException::class)
    fun mockAPI() {
        val secrets = SimpleSecrets();
        val apis: APISecrets = object : APISecrets(secrets) {
            @Throws(SecretsException::class)
            override fun getAPI(url: String): I_API<Response> {
                val secret = getSecret(url)
                return MockAPI(url, secret, HELLO_WORLD)
            }
        }

        val fakeToken = "world";
        secrets.setSecret("hello", fakeToken)
        apis.grant("$BASE_URL/health/", "hello")
        val i_api_0 = apis.getAPI("$BASE_URL/health/hello")

        assert(i_api_0 is MockAPI)
        val i_api_0_resp = i_api_0[null]
        assert(i_api_0_resp.body != null)
        val body_0 = i_api_0_resp.body!!.string()
        println("mock.api.0.body:$i_api_0_resp")
        assert(body_0.startsWith("{") && body_0.endsWith("}"))
        //        assert null != i_api_0_resp.header("Authorization");
//        assert i_api_0_resp.header("Authorization").substring(7).equals(authToken);
        val i_api_1 = apis.getAPI("$BASE_URL/health/service/world")
//        println("mock.api.1: " + i_api_1.authToken)
//        assert(null != i_api_1.authToken)
//        assert(i_api_1.authToken == "world")
        val i_api_2 = apis.getAPI("$BASE_URL/missing/") as MockAPI
        println("mock.api.2: $i_api_2")
        assert(null == i_api_2.secret)
    }
}