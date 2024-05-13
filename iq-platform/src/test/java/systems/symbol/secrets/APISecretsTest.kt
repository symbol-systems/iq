package systems.symbol.secrets

import okhttp3.Response
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.util.Values
import org.junit.jupiter.api.Test
import systems.symbol.agent.tools.APIException
import systems.symbol.agent.tools.I_API
import systems.symbol.agent.tools.MockAPI
import systems.symbol.COMMONS
import systems.symbol.platform.IQ_NS
import java.io.IOException

internal class APISecretsTest {
    var HELLO_WORLD: String = "{\"hello\": \"world\"}"
    var BASE_URL: String = "http://localhost:8080/"

    var self: IRI = Values.iri(IQ_NS.TEST)

    @Test
    @Throws(SecretsException::class, APIException::class, IOException::class)
    fun mockAPI() {
        val secrets = SimpleSecrets();
        val apis = APISecrets(secrets);

        val fakeToken = "world";
        secrets.setSecret("hello", fakeToken)
        apis.grant("$BASE_URL/health/", "hello")
        val i_api_0 = apis.getSecret("$BASE_URL/health/hello")
        assert(i_api_0 != null)
        assert(i_api_0.equals(fakeToken));
    }
}