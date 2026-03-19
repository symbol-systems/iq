package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;

/**
 * Represents a single connector session, including its active configuration and the model used to store state.
 */
public interface I_ConnectorSession {

/** The identity of the connector that owns this session. */
IRI getSelf();

/**
 * Returns a model that can be used to read/write connector state.
 *
 * <p>The model is expected to be backed by a repository or persisted store; implementations
 * can choose whether this is a read-only view or a writable model.</p>
 */
Model getModel();

/**
 * Removes any cached session resources (e.g., clients, temporary models).
 */
void close();
}
