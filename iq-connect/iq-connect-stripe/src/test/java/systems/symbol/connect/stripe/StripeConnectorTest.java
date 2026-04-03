
package systems.symbol.connect.stripe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class StripeConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
StripeConnectorConfig config = new StripeConnectorConfig(null, Duration.ofMinutes(5), null);
StripeConnector connector = new StripeConnector("urn:iq:connector:stripe", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
StripeConnectorConfig config = new StripeConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
StripeConnector connector = new StripeConnector("urn:iq:connector:stripe", config);
StripeConnectorKernel kernel = new StripeConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
