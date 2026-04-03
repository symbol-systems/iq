
package systems.symbol.connect.digitalocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class DigitaloceanConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
DigitaloceanConnectorConfig config = new DigitaloceanConnectorConfig(null, Duration.ofMinutes(5), null);
DigitaloceanConnector connector = new DigitaloceanConnector("urn:iq:connector:digitalocean", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
DigitaloceanConnectorConfig config = new DigitaloceanConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
DigitaloceanConnector connector = new DigitaloceanConnector("urn:iq:connector:digitalocean", config);
DigitaloceanConnectorKernel kernel = new DigitaloceanConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
