package systems.symbol.control.node;

/**
 * Represents the operational state of a node in the cluster.
 */
public enum ClusterNodeState {
/**
 * Node is healthy and accepting requests.
 */
HEALTHY,

/**
 * Node is degraded but still responding (high latency, high error rate).
 */
DEGRADED,

/**
 * Node has missed multiple heartbeat intervals; marked for removal.
 */
UNHEALTHY,

/**
 * Node is shutting down gracefully; no new requests should be routed to it.
 */
DRAINING,

/**
 * Node is offline/unreachable.
 */
OFFLINE
}
