package systems.symbol.connect.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.Modeller;

public class TemplateConnectorTest {

@Test
void testRefreshProducesTemplateItemAndCheckpoint() {
TemplateConnector connector = new TemplateConnector("urn:iq:connector:template");

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
assertFalse(connector.getCheckpoint().isPresent());

connector.refresh();

assertEquals(ConnectorStatus.IDLE, connector.getStatus());
assertTrue(connector.getCheckpoint().isPresent(), "Checkpoint should be created after successful refresh");

IRI expectedType = SimpleValueFactory.getInstance().createIRI(Modeller.getConnectOntology() + "TemplateItem");
IRI expectedGraph = SimpleValueFactory.getInstance().createIRI("urn:iq:connector:template/graph/current");

assertTrue(connector.getModel().filter(null, Modeller.rdfType(), expectedType, expectedGraph).iterator().hasNext(), "TemplateItem entity should be in state graph");
}
}