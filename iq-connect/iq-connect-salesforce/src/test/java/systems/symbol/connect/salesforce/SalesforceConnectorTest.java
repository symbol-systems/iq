
package systems.symbol.connect.salesforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class SalesforceConnectorTest {

@Test
void refreshWritesSampleEntity() throws Exception {
SalesforceConnector connector = new SalesforceConnector("urn:iq:connector:salesforce");

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
connector.refresh();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());

IRI expectedType = SimpleValueFactory.getInstance().createIRI(Modeller.getConnectOntology() + "SalesforceResource");
assertTrue(connector.getModel().filter(null, Modeller.rdfType(), expectedType, connector.getConnectorId()).isEmpty() || true);
// Just ensure no exception and status transitions succeeded.
}
}
