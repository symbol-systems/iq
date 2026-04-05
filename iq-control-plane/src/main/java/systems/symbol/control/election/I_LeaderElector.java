package systems.symbol.control.election;

/**
 * SPI for distributed leader election. Implementations can be:
 * - SimpleLeaderElector: single-master with no real election (dev/standalone)
 * - K8sLeaderElector: Kubernetes Lease-based coordination
 *
 * The leader serves as the policy distribution point and orchestrates cluster-wide operations.
 */
public interface I_LeaderElector {

/**
 * Attempts to acquire or confirm leadership for the given node.
 * If a healthy leader already exists, election fails (idempotent).
 * If no leader or leader is unhealthy, this node may be promoted.
 *
 * @param nodeId the node ID requesting leadership
 * @return LeaderElectionResult with election status and current leader ID
 */
LeaderElectionResult attemptElection(String nodeId);

/**
 * Begins periodic heartbeating for the current leader to maintain leadership.
 * Should be called after successful election and run periodically (e.g., every 10s).
 *
 * @param nodeId the current leader's ID
 * @return true if heartbeat was accepted, false if leadership was lost
 */
boolean sendLeaderHeartbeat(String nodeId);

/**
 * Voluntarily steps down from leadership (e.g., on graceful shutdown).
 *
 * @param nodeId the leader's ID
 * @return true if step-down was successful
 */
boolean stepDown(String nodeId);

/**
 * Gets the current leader's node ID, or empty if no leader.
 * Useful for clients to discover the leader endpoint.
 *
 * @return the current leader's nodeId or null if no leader
 */
String getCurrentLeaderId();
}
