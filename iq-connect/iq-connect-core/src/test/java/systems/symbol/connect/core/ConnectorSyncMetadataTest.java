package systems.symbol.connect.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

public class ConnectorSyncMetadataTest {

@Test
void testSyncMetadataLifecycleInGraphContext() {
Model model = new LinkedHashModel();
IRI connector = SimpleValueFactory.getInstance().createIRI("urn:connector:test");
IRI graph = SimpleValueFactory.getInstance().createIRI("urn:connector:test:graph");

ConnectorSyncMetadata.markSyncing(model, connector, graph);
assertTrue(model.contains(connector, Values.iri(ConnectorModels.HAS_SYNC_STATUS), Values.literal("SYNCING"), graph));
assertTrue(model.contains(connector, Values.iri(ConnectorModels.LAST_SYNCED_AT), null, graph));

ConnectorSyncMetadata.markSynced(model, connector, graph);
assertTrue(model.contains(connector, Values.iri(ConnectorModels.HAS_SYNC_STATUS), Values.literal("SYNCED"), graph));

ConnectorSyncMetadata.markError(model, connector, graph);
assertTrue(model.contains(connector, Values.iri(ConnectorModels.HAS_SYNC_STATUS), Values.literal("ERROR"), graph));
}
}
