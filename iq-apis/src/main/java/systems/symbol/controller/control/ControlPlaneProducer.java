package systems.symbol.controller.control;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import systems.symbol.control.election.I_LeaderElector;
import systems.symbol.control.election.SimpleLeaderElector;
import systems.symbol.control.node.I_NodeRegistry;
import systems.symbol.control.node.InMemoryNodeRegistry;
import systems.symbol.control.policy.I_PolicyDistributor;
import systems.symbol.control.policy.InMemoryPolicyDistributor;

import java.util.Optional;

/**
 * CDI producers for control plane components.
 * Wires node registry, leader election, and policy distribution.
 *
 * Configuration properties:
 * - iq.control.registry: registry type (memory|k8s), default: memory
 * - iq.control.election: election type (simple|k8s), default: simple
 * - iq.control.policy-distributor: distributor type (memory|distributed), default: memory
 * - iq.control.policy.hmac-key: base64-encoded 32-byte HMAC key for policy bundle signing
 * - iq.control.leader-election.heartbeat-timeout: milliseconds, default: 30000
 */
@ApplicationScoped
public class ControlPlaneProducer {

@ConfigProperty(name = "iq.control.registry", defaultValue = "memory")
Optional<String> registryType;

@ConfigProperty(name = "iq.control.election", defaultValue = "simple")
Optional<String> electionType;

@ConfigProperty(name = "iq.control.policy-distributor", defaultValue = "memory")
Optional<String> distributorType;

@ConfigProperty(name = "iq.control.policy.hmac-key")
Optional<String> hmacKeyBase64;

@ConfigProperty(name = "iq.control.leader-election.heartbeat-timeout", defaultValue = "30000")
long heartbeatTimeoutMs;

@Produces
@Singleton
public I_NodeRegistry nodeRegistry() {
String type = registryType.orElse("memory").toLowerCase();
return switch (type) {
case "memory" -> new InMemoryNodeRegistry();
case "k8s" -> {
// Future: K8sNodeRegistry using Kubernetes API
yield new InMemoryNodeRegistry();  // Fallback for now
}
default -> new InMemoryNodeRegistry();
};
}

@Produces
@Singleton
public I_LeaderElector leaderElector(I_NodeRegistry nodeRegistry) {
String type = electionType.orElse("simple").toLowerCase();
return switch (type) {
case "simple" -> new SimpleLeaderElector(nodeRegistry, heartbeatTimeoutMs);
case "k8s" -> {
// Future: K8sLeaderElector using Kubernetes Lease API
yield new SimpleLeaderElector(nodeRegistry, heartbeatTimeoutMs);  // Fallback
}
default -> new SimpleLeaderElector(nodeRegistry, heartbeatTimeoutMs);
};
}

@Produces
@Singleton
public I_PolicyDistributor policyDistributor() {
String type = distributorType.orElse("memory").toLowerCase();
byte[] hmacKey = extractHmacKey();

return switch (type) {
case "memory" -> new InMemoryPolicyDistributor(hmacKey, 86400);  // 24-hour bundle TTL
case "distributed" -> {
// Future: DistributedPolicyDistributor with leader→worker sync
yield new InMemoryPolicyDistributor(hmacKey, 86400);  // Fallback
}
default -> new InMemoryPolicyDistributor(hmacKey, 86400);
};
}

/**
 * Extracts and decodes the HMAC key from configuration.
 * Falls back to a default 32-byte key if not configured (development only).
 */
private byte[] extractHmacKey() {
if (hmacKeyBase64.isEmpty()) {
// Development fallback: create a deterministic key
byte[] key = new byte[32];
for (int i = 0; i < 32; i++) {
key[i] = (byte) (i & 0xFF);
}
return key;
}

try {
return java.util.Base64.getDecoder().decode(hmacKeyBase64.get());
} catch (IllegalArgumentException e) {
throw new RuntimeException("Invalid base64-encoded HMAC key in iq.control.policy.hmac-key", e);
}
}
}
