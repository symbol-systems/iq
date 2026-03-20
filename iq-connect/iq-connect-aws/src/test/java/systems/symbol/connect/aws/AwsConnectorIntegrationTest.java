package systems.symbol.connect.aws;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class AwsConnectorIntegrationTest {

    @Test
    void testAwsConnectorRefreshWithRealCredentials() {
        Assumptions.assumeTrue(System.getenv("AWS_ACCESS_KEY_ID") != null && System.getenv("AWS_SECRET_ACCESS_KEY") != null,
                "AWS credentials are required for integration test");

        AwsConfig config = new AwsConfig(System.getenv("AWS_REGION") == null ? "us-east-1" : System.getenv("AWS_REGION"));
        Model state = new LinkedHashModel();
        AwsConnector c = new AwsConnector("urn:aws:connector:test", config, state);

        c.refresh();

        assertTrue(c.getStatus() != null);
        assertTrue(state.contains(c.getConnectorId(), null, null), "State model should contain some triples after refresh");
    }
}
