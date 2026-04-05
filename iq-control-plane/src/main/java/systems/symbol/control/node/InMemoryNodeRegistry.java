package systems.symbol.control.node;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of I_NodeRegistry.
 * Thread-safe for concurrent reads; suitable for dev/standalone mode.
 * Not suitable for distributed deployments (use K8sNodeRegistry instead).
 */
public class InMemoryNodeRegistry implements I_NodeRegistry {

private final ConcurrentHashMap<String, ClusterNode> nodes = new ConcurrentHashMap<>();
private volatile String leaderId = null;

@Override
public void register(ClusterNode node) {
Objects.requireNonNull(node, "node");
nodes.put(node.nodeId(), node);
}

@Override
public boolean unregister(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");
ClusterNode removed = nodes.remove(nodeId);
if (nodeId.equals(leaderId)) {
leaderId = null;  // Demote deceased leader
}
return removed != null;
}

@Override
public Optional<ClusterNode> get(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");
return Optional.ofNullable(nodes.get(nodeId));
}

@Override
public Collection<ClusterNode> listAll() {
return Collections.unmodifiableCollection(new ArrayList<>(nodes.values()));
}

@Override
public Collection<ClusterNode> listByState(ClusterNodeState state) {
Objects.requireNonNull(state, "state");
return Collections.unmodifiableCollection(
nodes.values().stream()
.filter(n -> n.state() == state)
.toList()
);
}

@Override
public Optional<ClusterNode> findLeader() {
if (leaderId == null) {
return Optional.empty();
}
return Optional.ofNullable(nodes.get(leaderId));
}

@Override
public boolean promoteToLeader(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");
if (!nodes.containsKey(nodeId)) {
return false;
}
synchronized (this) {
// Demote previous leader
if (leaderId != null && !leaderId.equals(nodeId)) {
ClusterNode prevLeader = nodes.get(leaderId);
if (prevLeader != null) {
nodes.put(leaderId, prevLeader.withLeaderFlag(false));
}
}
// Promote new leader
leaderId = nodeId;
ClusterNode newLeader = nodes.get(nodeId);
nodes.put(nodeId, newLeader.withLeaderFlag(true));
}
return true;
}

@Override
public boolean updateNodeState(String nodeId, ClusterNodeState newState) {
Objects.requireNonNull(nodeId, "nodeId");
Objects.requireNonNull(newState, "newState");
ClusterNode current = nodes.get(nodeId);
if (current == null) {
return false;
}
nodes.put(nodeId, current.withStateAndHeartbeat(newState, Instant.now()));
return true;
}

@Override
public int countHealthyNodes() {
return (int) nodes.values().stream()
.filter(n -> n.state() == ClusterNodeState.HEALTHY)
.count();
}

@Override
public void clear() {
nodes.clear();
leaderId = null;
}

/**
 * For testing: explicitly sets a node as leader in registry.
 */
public void setLeaderForTest(String nodeId) {
promoteToLeader(nodeId);
}
}
