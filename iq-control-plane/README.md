# IQ Control Plane

Distributed cluster coordination for IQ: node registry, leader election, policy distribution, and mTLS node-to-node communication.

## Architecture

The control plane enables an IQ cluster to:

1. **Track cluster membership** — Node registry with health status
2. **Elect a leader** — Single master for orchestrating policy distribution
3. **Distribute policy** — Leader publishes signed policy bundles; workers validate and reload
4. **Communicate securely** — mTLS for node-to-node verification

## Core Interfaces

### `I_NodeRegistry`
Manages cluster node membership and state.

**Implementations:**
- `InMemoryNodeRegistry`: Single-machine, suitable for dev/standalone
- `K8sNodeRegistry`: Kubernetes Leases for persistent state (future)

**Key methods:**
```java
void register(ClusterNode node);
Optional<ClusterNode> get(String nodeId);
Collection<ClusterNode> listByState(ClusterNodeState state);
Optional<ClusterNode> findLeader();
boolean promoteToLeader(String nodeId);
```

### `I_LeaderElector`
Coordinates leader election among cluster nodes.

**Implementations:**
- `SimpleLeaderElector`: Single-master, no real consensus (dev/standalone)
- `K8sLeaderElector`: Kubernetes Lease-based coordination (future)

**Key methods:**
```java
LeaderElectionResult attemptElection(String nodeId);
boolean sendLeaderHeartbeat(String nodeId);
boolean stepDown(String nodeId);
String getCurrentLeaderId();
```

### `I_PolicyDistributor`
Leader publishes policy bundles; workers retrieve and verify.

**Implementations:**
- `InMemoryPolicyDistributor`: Single-machine shared storage (dev/standalone)
- `DistributedPolicyDistributor`: Worker-triggered distribution (future)

**Key methods:**
```java
long publishPolicyBundle(byte[] policyBytes);
Optional<SignedPolicyBundle> getLatestBundle();
boolean verifyBundleSignature(SignedPolicyBundle bundle);
void recordBundleAcceptance(String nodeId, long bundleVersion);
```

## Usage

### Registering a Node

```java
ClusterNode myNode = new ClusterNode(
"node-1",  // nodeId
"primary",  // nickname
"https://node-1.example.com",  // endpoint
Instant.now(),  // registered
Instant.now(),  // lastHeartbeat
false,  // leader
ClusterNodeState.HEALTHY// state
);
nodeRegistry.register(myNode);
```

### Electing a Leader

```java
I_LeaderElector elector = new SimpleLeaderElector(nodeRegistry);
LeaderElectionResult result = elector.attemptElection("node-1");
if (result.elected()) {
// This node is now leader; start heartbeat loop
scheduleLeaderHeartbeats("node-1", elector);
}
```

### Distributing Policy

```java
I_PolicyDistributor distributor = new InMemoryPolicyDistributor(hmacKey);

// Leader publishes policy
long version = distributor.publishPolicyBundle(policyRdfBytes);

// Worker retrieves and verifies
Optional<SignedPolicyBundle> bundle = distributor.getLatestBundle();
if (bundle.isPresent() && distributor.verifyBundleSignature(bundle.get())) {
reloadEnforcer(bundle.get().policyBytes());
distributor.recordBundleAcceptance(myNodeId, bundle.get().version());
}
```

## Configuration

In `application.properties`:

```properties
# Node identity
iq.node.id=node-1
iq.node.nickname=primary
iq.node.endpoint=https://node-1.example.com

# Leader election timeout (ms)
iq.election.heartbeat-timeout=30000

# Policy bundle TTL (seconds)
iq.policy.bundle-ttl=86400

# HMAC key for policy bundle signing (base64)
iq.policy.hmac-key=<32-byte key in base64>
```

## Security Considerations

- **Bundle Integrity**: Policy bundles are signed with HMAC-SHA256. Workers verify signatures before loading.
- **Version Tracking**: Bundle signatures include version number to prevent downgrade attacks.
- **mTLS**: Node-to-node communication uses mutual TLS (future: `K8sMTLSManager`).
- **Timing-Safe Comparison**: Signature verification uses constant-time byte array comparison.

## Testing

```bash
mvn -pl iq-control-plane -am test -DskipITs=true
```

Tests cover:
- Node registration and state transitions
- Leader election with health checks
- Policy bundle signing and verification
- Concurrent node registry access

## Future Work

- **K8sNodeRegistry**: Kubernetes Lease-based node registry
- **K8sLeaderElector**: Kubernetes Lease-based leader election
- **K8sMTLSManager**: Short-lived TLS certificates for node-to-node
- **Policy Acceleration**: Delta distribution for large policy bundles
- **Observability**: Metrics for election latency, bundle replication lag
