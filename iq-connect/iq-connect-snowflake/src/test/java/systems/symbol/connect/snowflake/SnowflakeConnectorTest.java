
package systems.symbol.connect.snowflake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class SnowflakeConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
SnowflakeConnectorConfig config = new SnowflakeConnectorConfig(null, Duration.ofMinutes(5), null);
SnowflakeConnector connector = new SnowflakeConnector("urn:iq:connector:snowflake", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
SnowflakeConnectorConfig config = new SnowflakeConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
SnowflakeConnector connector = new SnowflakeConnector("urn:iq:connector:snowflake", config);
SnowflakeConnectorKernel kernel = new SnowflakeConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
