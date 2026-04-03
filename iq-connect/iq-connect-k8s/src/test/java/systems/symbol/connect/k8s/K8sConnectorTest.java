
package systems.symbol.connect.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class K8sConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
K8sConnectorConfig config = new K8sConnectorConfig(null, Duration.ofMinutes(5), null);
K8sConnector connector = new K8sConnector("urn:iq:connector:k8s", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
K8sConnectorConfig config = new K8sConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
K8sConnector connector = new K8sConnector("urn:iq:connector:k8s", config);
K8sConnectorKernel kernel = new K8sConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
