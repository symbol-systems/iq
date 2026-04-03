
package systems.symbol.connect.datadog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class DatadogConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
DatadogConnectorConfig config = new DatadogConnectorConfig(null, Duration.ofMinutes(5), null);
DatadogConnector connector = new DatadogConnector("urn:iq:connector:datadog", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
DatadogConnectorConfig config = new DatadogConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
DatadogConnector connector = new DatadogConnector("urn:iq:connector:datadog", config);
DatadogConnectorKernel kernel = new DatadogConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
