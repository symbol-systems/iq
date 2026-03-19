package systems.symbol.connect.core;

/**
 * High-level status of a connector. This status is intended to be represented as RDF
 * in a connector's state graph so it can be queried and monitored.
 */
public enum ConnectorStatus {
UNKNOWN,
IDLE,
SYNCING,
PAUSED,
ERROR
}
