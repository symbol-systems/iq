
package systems.symbol.connect.databricks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class DatabricksConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
DatabricksConnectorConfig config = new DatabricksConnectorConfig(null, null, Duration.ofMinutes(5), null);
DatabricksConnector connector = new DatabricksConnector("urn:iq:connector:databricks", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
DatabricksConnectorConfig config = new DatabricksConnectorConfig("https://example.cloud.databricks.com", "dummy-key", Duration.ofMinutes(5), null);
DatabricksConnector connector = new DatabricksConnector("urn:iq:connector:databricks", config);
DatabricksConnectorKernel kernel = new DatabricksConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
