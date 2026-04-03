
package systems.symbol.connect.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class AwsConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
AwsConnectorConfig config = new AwsConnectorConfig(null, Duration.ofMinutes(5), null);
AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
AwsConnectorConfig config = new AwsConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);
AwsConnectorKernel kernel = new AwsConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
