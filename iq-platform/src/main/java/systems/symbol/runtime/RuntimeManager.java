package systems.symbol.runtime;

import systems.symbol.realm.Realm;

/**
 * Lightweight interface for runtime lifecycle management in the platform.
 * Implementations provided by iq-runtime module.
 */
public interface RuntimeManager {
/**
 * Initialize the runtime with a given realm context.
 */
void initialize(Realm realm) throws Exception;

/**
 * Start the runtime execution engine.
 */
void start() throws Exception;

/**
 * Stop the runtime gracefully.
 */
void stop() throws Exception;

/**
 * Check if runtime is currently active.
 */
boolean isActive();

/**
 * Get current runtime status.
 */
String getStatus();
}
