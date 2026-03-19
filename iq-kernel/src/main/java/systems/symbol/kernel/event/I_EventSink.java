package systems.symbol.kernel.event;

import systems.symbol.kernel.KernelException;

/**
 * Consumes {@link KernelEvent}s published to a topic.
 *
 * <p>Registered via {@link I_EventHub#subscribe(org.eclipse.rdf4j.model.IRI, I_EventSink)}.
 * Implementations may be synchronous or asynchronous depending on the hub
 * implementation.
 */
@FunctionalInterface
public interface I_EventSink {

    /**
     * Process an incoming event.
     *
     * @param event the event to process
     * @throws KernelException if processing fails unrecoverably
     */
    void accept(KernelEvent event) throws KernelException;
}
