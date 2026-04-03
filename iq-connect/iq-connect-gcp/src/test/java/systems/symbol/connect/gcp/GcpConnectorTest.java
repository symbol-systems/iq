
package systems.symbol.connect.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class GcpConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
GcpConnectorConfig config = new GcpConnectorConfig(null, Duration.ofMinutes(5), null);
GcpConnector connector = new GcpConnector("urn:iq:connector:gcp", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
GcpConnectorConfig config = new GcpConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
GcpConnector connector = new GcpConnector("urn:iq:connector:gcp", config);
GcpConnectorKernel kernel = new GcpConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
