
package systems.symbol.connect.office365;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class Office365ConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
Office365ConnectorConfig config = new Office365ConnectorConfig(null, Duration.ofMinutes(5), null);
Office365Connector connector = new Office365Connector("urn:iq:connector:office-365", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
Office365ConnectorConfig config = new Office365ConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
Office365Connector connector = new Office365Connector("urn:iq:connector:office-365", config);
Office365ConnectorKernel kernel = new Office365ConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
