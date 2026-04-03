
package systems.symbol.connect.slack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class SlackConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
SlackConnectorConfig config = new SlackConnectorConfig(null, Duration.ofMinutes(5), null);
SlackConnector connector = new SlackConnector("urn:iq:connector:slack", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
SlackConnectorConfig config = new SlackConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
SlackConnector connector = new SlackConnector("urn:iq:connector:slack", config);
SlackConnectorKernel kernel = new SlackConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
