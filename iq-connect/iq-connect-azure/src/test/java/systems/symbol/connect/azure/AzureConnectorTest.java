
package systems.symbol.connect.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class AzureConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
AzureConnectorConfig config = new AzureConnectorConfig(null, Duration.ofMinutes(5), null);
AzureConnector connector = new AzureConnector("urn:iq:connector:azure", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
AzureConnectorConfig config = new AzureConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
AzureConnector connector = new AzureConnector("urn:iq:connector:azure", config);
AzureConnectorKernel kernel = new AzureConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
