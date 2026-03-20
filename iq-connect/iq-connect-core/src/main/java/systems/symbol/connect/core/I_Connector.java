package systems.symbol.connect.core;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

/**
 * A connector is an active adapter that synchronizes a remote system into IQ.
 *
 * Connectors are expected to keep their own state as RDF and expose a live
 * {@link Model} view that can be queried by other IQ components.
 */
public interface I_Connector {

/** Returns the stable IRI that identifies this connector instance. */
IRI getConnectorId();

/** Returns the operational mode of the connector (read-only, write-only, etc.). */
ConnectorMode getMode();

/** Returns the current observed status of the connector (syncing, idle, error, etc.). */
ConnectorStatus getStatus();

/**
 * Returns a live view of the connector's internal state graph.
 *
 * <p>Implementations are free to return a {@link Model} backed by a repository or an
 * in-memory snapshot. The model should be considered the canonical view of the
 * connector's state.</p>
 */
Model getModel();

/** Returns an optional checkpoint that can be used to restore or resume sync. */
Optional<I_Checkpoint> getCheckpoint();

/** Starts or resumes the connector runtime. */
void start();

/** Stops or pauses the connector runtime. */
void stop();

/** Triggers an immediate sync cycle (read, write, or both) as supported by the connector. */
void refresh();
}
