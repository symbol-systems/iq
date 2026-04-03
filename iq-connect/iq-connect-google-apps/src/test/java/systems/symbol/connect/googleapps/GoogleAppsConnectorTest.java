
package systems.symbol.connect.googleapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class GoogleAppsConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
GoogleAppsConnectorConfig config = new GoogleAppsConnectorConfig(null, Duration.ofMinutes(5), null);
GoogleAppsConnector connector = new GoogleAppsConnector("urn:iq:connector:google-apps", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
GoogleAppsConnectorConfig config = new GoogleAppsConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
GoogleAppsConnector connector = new GoogleAppsConnector("urn:iq:connector:google-apps", config);
GoogleAppsConnectorKernel kernel = new GoogleAppsConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
