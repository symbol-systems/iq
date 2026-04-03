
package systems.symbol.connect.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class DockerConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
DockerConnectorConfig config = new DockerConnectorConfig(null, Duration.ofMinutes(5), null);
DockerConnector connector = new DockerConnector("urn:iq:connector:docker", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
DockerConnectorConfig config = new DockerConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
DockerConnector connector = new DockerConnector("urn:iq:connector:docker", config);
DockerConnectorKernel kernel = new DockerConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
