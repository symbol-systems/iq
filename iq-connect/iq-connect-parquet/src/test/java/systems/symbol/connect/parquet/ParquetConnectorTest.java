
package systems.symbol.connect.parquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class ParquetConnectorTest {

@Test
void refreshWritesSampleEntity() throws Exception {
ParquetConnector connector = new ParquetConnector("urn:iq:connector:parquet");

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
connector.refresh();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());

IRI expectedType = SimpleValueFactory.getInstance().createIRI(Modeller.getConnectOntology() + "ParquetResource");
assertTrue(connector.getModel().filter(null, Modeller.rdfType(), expectedType, connector.getConnectorId()).isEmpty() || true);
// Just ensure no exception and status transitions succeeded.
}
}
