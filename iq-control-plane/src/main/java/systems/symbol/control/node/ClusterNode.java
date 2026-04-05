package systems.symbol.control.node;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value type representing a node in the IQ cluster.
 * Nodes are identified by a unique nodeId and track health/leadership state.
 */
public final class ClusterNode {
private final String nodeId;
private final String nickname;
private final String endpoint;
private final Instant registered;
private final Instant lastHeartbeat;
private final boolean leader;
private final ClusterNodeState state;

public ClusterNode(String nodeId, String nickname, String endpoint, Instant registered,
   Instant lastHeartbeat, boolean leader, ClusterNodeState state) {
this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
this.nickname = Objects.requireNonNull(nickname, "nickname");
this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
this.registered = Objects.requireNonNull(registered, "registered");
this.lastHeartbeat = Objects.requireNonNull(lastHeartbeat, "lastHeartbeat");
this.leader = leader;
this.state = Objects.requireNonNull(state, "state");
}

public String nodeId() {
return nodeId;
}

public String nickname() {
return nickname;
}

public String endpoint() {
return endpoint;
}

public Instant registered() {
return registered;
}

public Instant lastHeartbeat() {
return lastHeartbeat;
}

public boolean isLeader() {
return leader;
}

public ClusterNodeState state() {
return state;
}

/**
 * Creates a copy with updated state and heartbeat time.
 */
public ClusterNode withStateAndHeartbeat(ClusterNodeState newState, Instant newHeartbeat) {
return new ClusterNode(nodeId, nickname, endpoint, registered, newHeartbeat, leader, newState);
}

/**
 * Creates a copy with updated leader flag.
 */
public ClusterNode withLeaderFlag(boolean newLeader) {
return new ClusterNode(nodeId, nickname, endpoint, registered, lastHeartbeat, newLeader, state);
}

@Override
public boolean equals(Object o) {
if (this == o) return true;
if (o == null || getClass() != o.getClass()) return false;
ClusterNode that = (ClusterNode) o;
return nodeId.equals(that.nodeId);
}

@Override
public int hashCode() {
return Objects.hash(nodeId);
}

@Override
public String toString() {
return "ClusterNode{" + "nodeId='" + nodeId + '\'' + ", nickname='" + nickname + '\''
   + ", endpoint='" + endpoint + '\'' + ", leader=" + leader + ", state=" + state + '}';
}
}
