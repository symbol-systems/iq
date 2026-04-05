package systems.symbol.control.node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryNodeRegistryTest {

private InMemoryNodeRegistry registry;
private ClusterNode node1;
private ClusterNode node2;

@BeforeEach
public void setUp() {
registry = new InMemoryNodeRegistry();
node1 = new ClusterNode("node-1", "primary", "https://node-1:8080",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
node2 = new ClusterNode("node-2", "secondary", "https://node-2:8080",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
}

@Test
public void testRegisterAndRetrieve() {
registry.register(node1);
var found = registry.get("node-1");
assertTrue(found.isPresent());
assertEquals("node-1", found.get().nodeId());
}

@Test
public void testUnregister() {
registry.register(node1);
assertTrue(registry.unregister("node-1"));
assertTrue(registry.get("node-1").isEmpty());
}

@Test
public void testListAll() {
registry.register(node1);
registry.register(node2);
var all = registry.listAll();
assertEquals(2, all.size());
}

@Test
public void testListByState() {
registry.register(node1);
registry.register(node2.withStateAndHeartbeat(ClusterNodeState.OFFLINE, Instant.now()));

var healthy = registry.listByState(ClusterNodeState.HEALTHY);
assertEquals(1, healthy.size());

var offline = registry.listByState(ClusterNodeState.OFFLINE);
assertEquals(1, offline.size());
}

@Test
public void testPromoteToLeader() {
registry.register(node1);
registry.register(node2);

assertTrue(registry.promoteToLeader("node-1"));

var leader = registry.findLeader();
assertTrue(leader.isPresent());
assertEquals("node-1", leader.get().nodeId());
assertTrue(leader.get().isLeader());
}

@Test
public void testPromoteToLeaderDemotesPrevious() {
registry.register(node1);
registry.register(node2);

registry.promoteToLeader("node-1");
var n1 = registry.get("node-1").get();
assertTrue(n1.isLeader());

registry.promoteToLeader("node-2");
var n1After = registry.get("node-1").get();
assertFalse(n1After.isLeader());

var n2 = registry.get("node-2").get();
assertTrue(n2.isLeader());
}

@Test
public void testUpdateNodeState() {
registry.register(node1);

registry.updateNodeState("node-1", ClusterNodeState.DEGRADED);
var updated = registry.get("node-1").get();
assertEquals(ClusterNodeState.DEGRADED, updated.state());
}

@Test
public void testCountHealthyNodes() {
registry.register(node1);
registry.register(node2);
registry.register(new ClusterNode("node-3", "tertiary", "https://node-3:8080",
Instant.now(), Instant.now(), false, ClusterNodeState.OFFLINE));

assertEquals(2, registry.countHealthyNodes());
}

@Test
public void testUnregisterLeaderClearsLeadership() {
registry.register(node1);
registry.promoteToLeader("node-1");
assertTrue(registry.findLeader().isPresent());

registry.unregister("node-1");
assertTrue(registry.findLeader().isEmpty());
}

@Test
public void testClear() {
registry.register(node1);
registry.register(node2);
registry.promoteToLeader("node-1");

registry.clear();

assertTrue(registry.listAll().isEmpty());
assertTrue(registry.findLeader().isEmpty());
}
}
