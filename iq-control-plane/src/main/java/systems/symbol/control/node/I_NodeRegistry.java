package systems.symbol.control.node;

import java.util.Collection;
import java.util.Optional;

/**
 * SPI for cluster node registry. Implementations can be:
 * - InMemoryNodeRegistry: dev/standalone mode
 * - K8sNodeRegistry: Kubernetes leases for persistent state
 *
 * Implementations must be thread-safe and support concurrent reads.
 */
public interface I_NodeRegistry {

/**
 * Registers or updates a node in the cluster.
 * If the node already exists (same nodeId), it is updated with new endpoint/state.
 *
 * @param node the node to register/update
 * @throws RuntimeException if registration fails
 */
void register(ClusterNode node);

/**
 * Unregisters a node from the cluster (e.g., on graceful shutdown).
 *
 * @param nodeId the node ID to unregister
 * @return true if the node was found and removed, false if not found
 */
boolean unregister(String nodeId);

/**
 * Retrieves a single node by its ID.
 *
 * @param nodeId the node ID
 * @return Optional containing the node if found
 */
Optional<ClusterNode> get(String nodeId);

/**
 * Lists all registered nodes in the cluster.
 *
 * @return immutable collection of all nodes
 */
Collection<ClusterNode> listAll();

/**
 * Lists all nodes with a specific state.
 *
 * @param state the desired state (e.g., HEALTHY)
 * @return immutable collection of matching nodes
 */
Collection<ClusterNode> listByState(ClusterNodeState state);

/**
 * Finds the current cluster leader.
 *
 * @return Optional containing the leader node if one exists
 */
Optional<ClusterNode> findLeader();

/**
 * Marks a node as leader (typically called by leader election).
 * Only one node should be leader at a time; previous leader is demoted.
 *
 * @param nodeId the node ID to promote to leader
 * @return true if the node was promoted, false if node not found
 */
boolean promoteToLeader(String nodeId);

/**
 * Updates a node's state and heartbeat timestamp.
 *
 * @param nodeId the node ID
 * @param newState the new state
 * @return true if updated, false if node not found
 */
boolean updateNodeState(String nodeId, ClusterNodeState newState);

/**
 * Returns the number of healthy nodes in the cluster.
 */
int countHealthyNodes();

/**
 * Clears all nodes (for testing or hard reset).
 */
void clear();
}
