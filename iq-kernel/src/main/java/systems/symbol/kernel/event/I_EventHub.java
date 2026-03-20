package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.KernelException;

/**
 * Publish-subscribe event bus for the kernel.
 *
 * <p>Decouples event producers and consumers from the transport mechanism.
 * Implementations:
 * <ul>
 *   <li>{@link SimpleEventHub} — in-process, synchronous (tests and CLI)</li>
 *   <li>{@code CamelEventHub} in {@code iq-camel} — wraps {@code ProducerTemplate}</li>
 *   <li>{@code VertxEventHub} in {@code iq-apis} — wraps Quarkus event bus</li>
 * </ul>
 *
 * <p>The interface is intentionally minimal; advanced routing, filtering,
 * and back-pressure are left to the surface implementation.
 */
public interface I_EventHub {

/**
 * Publish an event to all subscribers of {@code event.getTopic()}.
 *
 * @param event the event to publish
 * @throws KernelException if the event cannot be delivered
 */
void publish(KernelEvent event) throws KernelException;

/**
 * Register a sink to receive events on the given topic IRI.
 *
 * @param topic the topic to subscribe to
 * @param sink  the receiver
 */
void subscribe(IRI topic, I_EventSink sink);

/**
 * Remove all subscriptions for the given sink on the given topic.
 * No-op if the sink was not subscribed to that topic.
 */
void unsubscribe(IRI topic, I_EventSink sink);
}
