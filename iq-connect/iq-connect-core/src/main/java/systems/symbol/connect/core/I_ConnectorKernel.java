package systems.symbol.connect.core;

import java.util.concurrent.CompletableFuture;

/**
 * A running connector instance that manages the sync loop.
 *
 * The kernel is responsible for driving the connector state forward (polling, applying, error recovery) and
 * for publishing state changes into the connector model.
 */
public interface I_ConnectorKernel {

    /**
     * Starts the kernel.
     *
     * <p>The kernel should be idempotent: calling start more than once should be safe.
     */
    CompletableFuture<Void> start();

    /**
     * Stops the kernel and releases any held resources.
     */
    CompletableFuture<Void> stop();

    /**
     * Triggers an immediate sync and returns when the cycle completes.
     */
    CompletableFuture<Void> refresh();

    /** Returns a read-only view of the connector state. */
    I_Connector getConnector();

    /** Returns the descriptor for the connector that this kernel is running. */
    I_ConnectorDescriptor getDescriptor();

}
