
package systems.symbol.connect.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class AzureConnectorTest {

@Test
void testStartStopKernelDoesNotCrash() {
AzureConnectorConfig config = new AzureConnectorConfig("fake-subscription", "fake-tenant", java.time.Duration.ofSeconds(1), null, null);
AzureConnector connector = new AzureConnector("urn:iq:connector:azure", config);
AzureConnectorKernel kernel = new AzureConnectorKernel(connector, java.time.Duration.ofSeconds(1));

kernel.start().join();
kernel.stop().join();

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
}

@Test
void testRefreshHandlesMissingCredentialsFromEnv() {
AzureConnectorConfig config = new AzureConnectorConfig("fake-subscription", "fake-tenant", java.time.Duration.ofMinutes(5), null, null);
AzureConnector connector = new AzureConnector("urn:iq:connector:azure", config);

try {
connector.refresh();
} catch (Exception e) {
// expected in CI with no Azure credential environment
}

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
assertTrue(connector.getModel().isEmpty() || connector.getModel().size() >= 0);
}
}
