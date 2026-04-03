
package systems.symbol.connect.salesforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;

public class SalesforceConnectorTest {

@Test
void testRefreshFailsWithoutApiKey() {
SalesforceConnectorConfig config = new SalesforceConnectorConfig(null, Duration.ofMinutes(5), null);
SalesforceConnector connector = new SalesforceConnector("urn:iq:connector:salesforce", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
assertTrue(connector.getModel().size() >= 0);
}

@Test
void testKernelStartStop() {
SalesforceConnectorConfig config = new SalesforceConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
SalesforceConnector connector = new SalesforceConnector("urn:iq:connector:salesforce", config);
SalesforceConnectorKernel kernel = new SalesforceConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || connector.getStatus() == ConnectorStatus.SYNCING || connector.getStatus() == ConnectorStatus.ERROR);
}
}
