package systems.symbol.connect.core;

import java.time.Instant;
import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;

/**
 * Snapshot of the connector's sync state that can be persisted as RDF.
 */
public interface I_SyncState {

    /** When the connector last wrote its state. */
    Instant getLastSyncedAt();

    /** When the connector last attempted to poll or read remote state. */
    Instant getLastPolledAt();

    /** When the last error occurred, if any. */
    Optional<Instant> getLastErrorAt();

    /** The optional opaque checkpoint/cursor for the connector. */
    Optional<IRI> getCheckpoint();

    /** The current high-level status. */
    ConnectorStatus getStatus();
}
