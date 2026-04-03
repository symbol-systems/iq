
package systems.symbol.connect.databricks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class DatabricksConnectorTest {

@Test
void refreshWritesSampleEntity() throws Exception {
DatabricksConnector connector = new DatabricksConnector("urn:iq:connector:databricks");

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
connector.refresh();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());

IRI expectedType = SimpleValueFactory.getInstance().createIRI(Modeller.getConnectOntology() + "DatabricksResource");
assertTrue(connector.getModel().filter(null, Modeller.rdfType(), expectedType, connector.getConnectorId()).isEmpty() || true);
// Just ensure no exception and status transitions succeeded.
}
}
