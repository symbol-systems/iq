package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime-safe wrapper around an {@link I_EventHub}.
 *
 * <p>Pathological listeners (exceptions) are logged and suppressed so core
 * logic continues.
 */
public class SafeEventHub implements I_EventHub {

private static final Logger log = LoggerFactory.getLogger(SafeEventHub.class);

private final I_EventHub delegate;
private final Map<String, CopyOnWriteArrayList<I_EventSink>> subscriptions =
new ConcurrentHashMap<>();

public SafeEventHub(I_EventHub delegate) {
this.delegate = delegate == null ? new NoopEventHub() : delegate;
}

@Override
public void publish(KernelEvent event) {
// Delegate for optional cross-process transport; swallow any errors.
try {
delegate.publish(event);
} catch (Exception e) {
log.warn("safe-event-hub: delegate publish failure for topic {}: {}", 
event.getTopic(), e.getMessage(), e);
}

var topicKey = event.getTopic().stringValue();
var sinks = subscriptions.getOrDefault(topicKey, new CopyOnWriteArrayList<>());

for (I_EventSink sink : sinks) {
try {
sink.accept(event);
} catch (Exception e) {
log.warn("safe-event-hub: sink failure for topic {}: {}", topicKey, e.getMessage(), e);
}
}
}

@Override
public void subscribe(IRI topic, I_EventSink sink) {
subscriptions.computeIfAbsent(topic.stringValue(), k -> new CopyOnWriteArrayList<>()).add(sink);
try {
delegate.subscribe(topic, sink);
} catch (Exception e) {
log.warn("safe-event-hub: delegate subscribe failure for topic {}: {}", topic, e.getMessage(), e);
}
}

@Override
public void unsubscribe(IRI topic, I_EventSink sink) {
var sinks = subscriptions.get(topic.stringValue());
if (sinks != null) {
sinks.remove(sink);
}
try {
delegate.unsubscribe(topic, sink);
} catch (Exception e) {
log.warn("safe-event-hub: delegate unsubscribe failure for topic {}: {}", topic, e.getMessage(), e);
}
}
}
