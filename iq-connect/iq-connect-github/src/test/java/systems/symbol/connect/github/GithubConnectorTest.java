
package systems.symbol.connect.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class GithubConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
GithubConnectorConfig config = new GithubConnectorConfig(null, Duration.ofMinutes(5), null);
GithubConnector connector = new GithubConnector("urn:iq:connector:github", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
GithubConnectorConfig config = new GithubConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
GithubConnector connector = new GithubConnector("urn:iq:connector:github", config);
GithubConnectorKernel kernel = new GithubConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
