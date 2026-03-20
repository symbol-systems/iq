package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;

/**
 * Event hub implementation that ignores all events.
 */
public class NoopEventHub implements I_EventHub {

    @Override
    public void publish(KernelEvent event) {
        // no-op
    }

    @Override
    public void subscribe(IRI topic, I_EventSink sink) {
        // no-op
    }

    @Override
    public void unsubscribe(IRI topic, I_EventSink sink) {
        // no-op
    }
}
