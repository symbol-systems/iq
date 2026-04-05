package systems.symbol.control.election;

import systems.symbol.control.node.I_NodeRegistry;
import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Simple single-master leader election for dev/standalone mode.
 * Does not use real distributed consensus; suitable only for non-distributed deployments.
 * For production, use K8sLeaderElector with Kubernetes Lease API.
 *
 * Rules:
 * - At most one leader at a time
 * - Leader must be in HEALTHY state
 * - If leader misses heartbeat for 30s, it is demoted
 * - New election happens when no current leader
 */
public class SimpleLeaderElector implements I_LeaderElector {

private final I_NodeRegistry nodeRegistry;
private final long heartbeatTimeoutMillis;  // 30s default

public SimpleLeaderElector(I_NodeRegistry nodeRegistry) {
this(nodeRegistry, 30_000);  // 30 second timeout
}

public SimpleLeaderElector(I_NodeRegistry nodeRegistry, long heartbeatTimeoutMillis) {
this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
}

@Override
public synchronized LeaderElectionResult attemptElection(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");

// Check if this node exists
ClusterNode candidate = nodeRegistry.get(nodeId).orElse(null);
if (candidate == null) {
return LeaderElectionResult.failed(null, Instant.now(), "Node not found in registry");
}

// Check for existing healthy leader
if (isLeaderHealthy()) {
ClusterNode currentLeader = nodeRegistry.findLeader().orElse(null);
String leaderId = currentLeader != null ? currentLeader.nodeId() : null;
return LeaderElectionResult.alreadyLeader(leaderId, Instant.now());
}

// Demote unhealthy leader if any
nodeRegistry.findLeader().ifPresent(leader -> {
nodeRegistry.updateNodeState(leader.nodeId(), ClusterNodeState.UNHEALTHY);
});

// Promote this node to leader
nodeRegistry.promoteToLeader(nodeId);
return LeaderElectionResult.elected(nodeId, Instant.now());
}

@Override
public synchronized boolean sendLeaderHeartbeat(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");

ClusterNode current = nodeRegistry.get(nodeId).orElse(null);
if (current == null || !current.isLeader()) {
return false;  // Not the leader
}

// Update heartbeat
nodeRegistry.updateNodeState(nodeId, ClusterNodeState.HEALTHY);
return true;
}

@Override
public synchronized boolean stepDown(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");

ClusterNode current = nodeRegistry.get(nodeId).orElse(null);
if (current == null || !current.isLeader()) {
return false;  // Not the leader
}

// Demote to HEALTHY (not OFFLINE) to allow graceful transition
nodeRegistry.promoteToLeader(""); // Clears leader by promoting invalid ID
return true;
}

@Override
public String getCurrentLeaderId() {
return nodeRegistry.findLeader()
.map(ClusterNode::nodeId)
.orElse(null);
}

/**
 * Checks if there is a healthy leader and it hasn't missed heartbeat.
 */
private boolean isLeaderHealthy() {
return nodeRegistry.findLeader().map(leader -> {
if (leader.state() != ClusterNodeState.HEALTHY) {
return false;
}
// Check heartbeat timeout
long msSinceHeartbeat = ChronoUnit.MILLIS.between(leader.lastHeartbeat(), Instant.now());
return msSinceHeartbeat < heartbeatTimeoutMillis;
}).orElse(false);
}
}
