package systems.symbol.connect.core;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;

/**
 * Registry for connectors.
 *
 * <p>The registry is responsible for storing connector descriptors and providing
 * access to active connector instances.</p>
 */
public interface I_ConnectorRegistry {

/** Register a connector instance with the registry. */
void register(I_Connector connector);

/** Unregister a connector by its id. */
void unregister(IRI connectorId);

/**
 * Returns a connector instance if one is registered for the given id.
 */
Optional<I_Connector> get(IRI connectorId);

/**
 * Returns all registered connectors.
 */
Iterable<I_Connector> list();
}
