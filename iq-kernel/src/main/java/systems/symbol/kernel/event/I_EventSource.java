package systems.symbol.kernel.event;

import systems.symbol.kernel.KernelException;

/**
 * Produces {@link KernelEvent}s and publishes them to an {@link I_EventHub}.
 *
 * <p>Domain services (ingestors, agents) implement this interface to emit
 * events without depending on Camel, Vert.x, or any other event bus.
 */
public interface I_EventSource {

/**
 * Emits an event to the provided hub.
 *
 * @param hub   the hub to publish to
 * @param event the event to publish
 * @throws KernelException if the event cannot be emitted
 */
void emit(I_EventHub hub, KernelEvent event) throws KernelException;
}
