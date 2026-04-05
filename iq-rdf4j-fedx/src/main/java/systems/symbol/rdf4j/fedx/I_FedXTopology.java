package systems.symbol.rdf4j.fedx;

import java.util.Collection;
import java.util.Optional;

/**
 * SPI for federated SPARQL topology.
 * Implementations provide a view of available SPARQL endpoints in the federation.
 *
 * Implementations can be:
 * - StaticFedXTopology: fixed list of endpoints
 * - DynamicFedXTopology: uses Control Plane node registry to discover endpoints
 * - CachedFedXTopology: caches topology with periodic refresh
 */
public interface I_FedXTopology {

/**
 * Lists all available SPARQL endpoints in the federation.
 *
 * @return immutable collection of federated endpoints
 */
Collection<FedXEndpoint> getEndpoints();

/**
 * Gets a specific endpoint by node ID.
 *
 * @param nodeId the node ID
 * @return Optional containing the endpoint if found
 */
Optional<FedXEndpoint> getEndpoint(String nodeId);

/**
 * Gets all queryable endpoints (read-only SPARQL endpoints).
 *
 * @return immutable collection of queryable endpoints
 */
Collection<FedXEndpoint> getQueryableEndpoints();

/**
 * Gets the local endpoint (this node's SPARQL service).
 * Used to avoid querying ourselves in federated queries.
 *
 * @return Optional containing the local endpoint if defined
 */
Optional<FedXEndpoint> getLocalEndpoint();

/**
 * Checks if the topology has any endpoints.
 */
boolean isEmpty();

/**
 * Returns the number of endpoints in the topology.
 */
int size();
}
