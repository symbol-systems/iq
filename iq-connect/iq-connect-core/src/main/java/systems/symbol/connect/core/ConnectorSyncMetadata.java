package systems.symbol.connect.core;

import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Helper methods for connector synchronization metadata in connector models.
 */
public final class ConnectorSyncMetadata {

private ConnectorSyncMetadata() {
// utility
}

public static void markSyncing(Model model, IRI connectorId, Resource... contexts) {
upsertStatus(model, connectorId, "SYNCING", contexts);
}

public static void markSynced(Model model, IRI connectorId, Resource... contexts) {
upsertStatus(model, connectorId, "SYNCED", contexts);
}

public static void markError(Model model, IRI connectorId, Resource... contexts) {
upsertStatus(model, connectorId, "ERROR", contexts);
}

private static void upsertStatus(Model model, IRI connectorId, String status, Resource... contexts) {
IRI statusPredicate = Values.iri(ConnectorModels.HAS_SYNC_STATUS);
IRI lastSyncedPredicate = Values.iri(ConnectorModels.LAST_SYNCED_AT);

model.remove(connectorId, statusPredicate, null, contexts);
model.remove(connectorId, lastSyncedPredicate, null, contexts);

model.add(connectorId, statusPredicate, Values.literal(status), contexts);
model.add(connectorId, lastSyncedPredicate, SimpleValueFactory.getInstance().createLiteral(Instant.now().toString()), contexts);
}
}
