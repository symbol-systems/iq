package systems.symbol.rdf4j.fedx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;
import systems.symbol.control.node.InMemoryNodeRegistry;

import java.time.Instant;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for federated SPARQL topology discovery and endpoint management.
 */
public class FedXTopologyTest {

private InMemoryNodeRegistry nodeRegistry;
private StaticFedXTopology topology;

@BeforeEach
public void setUp() {
nodeRegistry = new InMemoryNodeRegistry();
topology = new StaticFedXTopology(nodeRegistry, "node-1");

// Register some cluster nodes
var node1 = new ClusterNode("node-1", "primary", "https://node-1.example.com",
Instant.now(), Instant.now(), true, ClusterNodeState.HEALTHY);
var node2 = new ClusterNode("node-2", "secondary", "https://node-2.example.com",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
var node3 = new ClusterNode("node-3", "tertiary", "https://node-3.example.com",
Instant.now(), Instant.now(), false, ClusterNodeState.OFFLINE);

nodeRegistry.register(node1);
nodeRegistry.register(node2);
nodeRegistry.register(node3);

// Refresh topology to pick up nodes
topology.refresh();
}

@Test
public void testTopologyDiscovery() {
Collection<FedXEndpoint> endpoints = topology.getEndpoints();
assertEquals(2, endpoints.size());  // Only HEALTHY nodes
}

@Test
public void testLocalEndpoint() {
var local = topology.getLocalEndpoint();
assertTrue(local.isPresent());
assertEquals("node-1", local.get().nodeId());
}

@Test
public void testRemoteEndpoints() {
var remote = topology.getRemoteEndpoints();
assertEquals(1, remote.size());  // Only node-2 (not node-1, not offline node-3)
assertTrue(remote.stream().anyMatch(ep -> ep.nodeId().equals("node-2")));
}

@Test
public void testQueryableEndpoints() {
var queryable = topology.getQueryableEndpoints();
assertEquals(2, queryable.size());
assertTrue(queryable.stream().allMatch(FedXEndpoint::isQueryable));
}

@Test
public void testEndpointLookup() {
var ep1 = topology.getEndpoint("node-1");
assertTrue(ep1.isPresent());
assertEquals("node-1", ep1.get().nodeId());

var ep3 = topology.getEndpoint("node-3");
assertFalse(ep3.isPresent());  // Offline node not in topology
}

@Test
public void testTopologyEmpty() {
var emptyReg = new InMemoryNodeRegistry();
var emptyTopo = new StaticFedXTopology(emptyReg, "node-1");
assertTrue(emptyTopo.isEmpty());
assertEquals(0, emptyTopo.size());
}

@Test
public void testTopologyRefresh() {
// Initially 2 endpoints
assertEquals(2, topology.size());

// Add a new healthy node
var newNode = new ClusterNode("node-4", "new", "https://node-4.example.com",
Instant.now(), Instant.now(), false, ClusterNodeState.HEALTHY);
nodeRegistry.register(newNode);

// Refresh topology
topology.refresh();

// Should now have 3 endpoints
assertEquals(3, topology.size());
}

@Test
public void testSPARQLEndpointURL() {
var ep = topology.getEndpoint("node-1").get();
assertEquals("https://node-1.example.com/sparql", ep.sparqlEndpoint());
}

@Test
public void testEndpointImmutability() {
Collection<FedXEndpoint> collection1 = topology.getEndpoints();
Collection<FedXEndpoint> collection2 = topology.getEndpoints();

// Both should be unmodifiable
assertThrows(UnsupportedOperationException.class, () -> collection1.add(null));
assertThrows(UnsupportedOperationException.class, () -> collection2.add(null));
}
}
