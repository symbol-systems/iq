package systems.symbol.rdf4j.fedx;

import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;
import systems.symbol.control.node.I_NodeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Static FedX topology using Control Plane node registry.
 * Discovers SPARQL endpoints from healthy cluster nodes.
 *
 * Rule: Only nodes in HEALTHY state are included in the topology.
 * Nodes are included if they respond to HTTP health checks at {endpoint}/sparql.
 */
public class StaticFedXTopology implements I_FedXTopology {

private final I_NodeRegistry nodeRegistry;
private final String localNodeId;
private final Map<String, FedXEndpoint> endpoints = new ConcurrentHashMap<>();

public StaticFedXTopology(I_NodeRegistry nodeRegistry, String localNodeId) {
this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
this.localNodeId = Objects.requireNonNull(localNodeId, "localNodeId");
refresh();
}

/**
 * Refreshes the topology from the node registry.
 * Call this periodically to pick up new nodes or state changes.
 */
public void refresh() {
endpoints.clear();

// Include all healthy nodes as SPARQL endpoints
nodeRegistry.listByState(ClusterNodeState.HEALTHY).forEach(node -> {
FedXEndpoint endpoint = new FedXEndpoint(
node.nodeId(),
node.endpoint(),
node.endpoint().endsWith("/") ? 
node.endpoint() + "sparql" : 
node.endpoint() + "/sparql",
true,   // queryable
false   // updateable (read-only federation)
);
endpoints.put(node.nodeId(), endpoint);
});
}

@Override
public Collection<FedXEndpoint> getEndpoints() {
return Collections.unmodifiableCollection(endpoints.values());
}

@Override
public Optional<FedXEndpoint> getEndpoint(String nodeId) {
Objects.requireNonNull(nodeId, "nodeId");
return Optional.ofNullable(endpoints.get(nodeId));
}

@Override
public Collection<FedXEndpoint> getQueryableEndpoints() {
return getEndpoints().stream()
.filter(FedXEndpoint::isQueryable)
.collect(Collectors.toUnmodifiableSet());
}

@Override
public Optional<FedXEndpoint> getLocalEndpoint() {
return getEndpoint(localNodeId);
}

@Override
public boolean isEmpty() {
return endpoints.isEmpty();
}

@Override
public int size() {
return endpoints.size();
}

/**
 * Gets endpoints excluding the local node (to avoid self-queries in federation).
 */
public Collection<FedXEndpoint> getRemoteEndpoints() {
return getEndpoints().stream()
.filter(ep -> !ep.nodeId().equals(localNodeId))
.collect(Collectors.toUnmodifiableSet());
}
}
