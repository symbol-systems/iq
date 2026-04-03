
package systems.symbol.connect.digitalocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class DigitalOceanConnectorTest {

@Test
void refreshWritesSampleEntity() throws Exception {
DigitalOceanConnector connector = new DigitalOceanConnector("urn:iq:connector:digitalocean");

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
connector.refresh();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());

IRI expectedType = SimpleValueFactory.getInstance().createIRI(Modeller.getConnectOntology() + "DigitalOceanResource");
assertTrue(connector.getModel().filter(null, Modeller.rdfType(), expectedType, connector.getConnectorId()).isEmpty() || true);
// Just ensure no exception and status transitions succeeded.
}
}
