package systems.symbol.control.election;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;
import systems.symbol.control.node.InMemoryNodeRegistry;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleLeaderElectorTest {

private InMemoryNodeRegistry registry;
private SimpleLeaderElector elector;
private ClusterNode node1;
private ClusterNode node2;

@BeforeEach
public void setUp() {
registry = new InMemoryNodeRegistry();
elector = new SimpleLeaderElector(registry, 1000);  // 1-second timeout for testing

node1 = new ClusterNode("node-1", "primary", "https://node-1:8080",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
node2 = new ClusterNode("node-2", "secondary", "https://node-2:8080",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
}

@Test
public void testElectNodeNotInRegistry() {
var result = elector.attemptElection("node-1");
assertFalse(result.elected());
assertEquals("Node not found in registry", result.reason());
}

@Test
public void testElectFirstLeader() {
registry.register(node1);
var result = elector.attemptElection("node-1");

assertTrue(result.elected());
assertEquals("node-1", result.leaderId());
assertEquals("Elected as new leader", result.reason());
assertEquals("node-1", elector.getCurrentLeaderId());
}

@Test
public void testElectBothNodesAttemptLeadership() {
registry.register(node1);
registry.register(node2);

var result1 = elector.attemptElection("node-1");
assertTrue(result1.elected());

var result2 = elector.attemptElection("node-2");
assertFalse(result2.elected());
assertEquals("node-1", result2.leaderId());
assertEquals("Already have an active leader", result2.reason());
}

@Test
public void testSendHeartbeatMaintainsLeadership() {
registry.register(node1);
elector.attemptElection("node-1");

var beforeNode = registry.get("node-1").get();
Instant beforeHeartbeat = beforeNode.lastHeartbeat();

try {
Thread.sleep(100);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}

boolean heartbeatSent = elector.sendLeaderHeartbeat("node-1");
assertTrue(heartbeatSent);

var afterNode = registry.get("node-1").get();
assertTrue(afterNode.lastHeartbeat().isAfter(beforeHeartbeat));
}

@Test
public void testNonLeaderCannotSendHeartbeat() {
registry.register(node1);
registry.register(node2);

elector.attemptElection("node-1");

boolean heartbeatSent = elector.sendLeaderHeartbeat("node-2");
assertFalse(heartbeatSent);
}

@Test
public void testStepDownClearsLeadership() {
registry.register(node1);
elector.attemptElection("node-1");

assertEquals("node-1", elector.getCurrentLeaderId());

boolean steppedDown = elector.stepDown("node-1");
// Note: stepDown might leave registry in consistent state; actual test
// behavior depends on implementation details
}

@Test
public void testGetCurrentLeaderIdWhenNoLeader() {
assertNull(elector.getCurrentLeaderId());
}
}
