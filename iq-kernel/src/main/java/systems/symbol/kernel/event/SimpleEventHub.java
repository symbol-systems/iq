package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process synchronous event hub — the default implementation.
 *
 * <p>Suitable for tests, CLI sessions, and any single-process deployment where
 * cross-JVM delivery is not required. Delivery is synchronous and ordered;
 * exceptions from a sink are logged and rethrown, stopping delivery to
 * remaining sinks for that publish call.
 *
 * <p>Thread-safe: concurrent publishes and subscribe/unsubscribe are safe.
 * Each subscriber list is a {@link CopyOnWriteArrayList} so subscribe /
 * unsubscribe during active iteration is safe.
 */
public class SimpleEventHub implements I_EventHub {

private static final Logger log = LoggerFactory.getLogger(SimpleEventHub.class);

private final Map<String, CopyOnWriteArrayList<I_EventSink>> subscriptions
= new ConcurrentHashMap<>();

@Override
public void publish(KernelEvent event) throws KernelException {
String topicKey = event.getTopic().stringValue();
List<I_EventSink> sinks = subscriptions.getOrDefault(
topicKey, new CopyOnWriteArrayList<>());

log.debug("event.publish: {} -> {} subscriber(s)", event, sinks.size());

for (I_EventSink sink : sinks) {
try {
sink.accept(event);
} catch (KernelException ke) {
log.error("event.sink.error: {} -> {}", event.getTopic(), ke.getCode(), ke);
throw ke;
} catch (Exception e) {
log.error("event.sink.fatal: {} -> {}", event.getTopic(), e.getMessage(), e);
throw new KernelException("kernel.event.sink", e.getMessage(), e);
}
}
}

@Override
public void subscribe(IRI topic, I_EventSink sink) {
subscriptions.computeIfAbsent(
topic.stringValue(), k -> new CopyOnWriteArrayList<>()).add(sink);
log.debug("event.subscribe: {} -> {}", topic, sink.getClass().getSimpleName());
}

@Override
public void unsubscribe(IRI topic, I_EventSink sink) {
CopyOnWriteArrayList<I_EventSink> sinks = subscriptions.get(topic.stringValue());
if (sinks != null) {
sinks.remove(sink);
log.debug("event.unsubscribe: {} -> {}", topic, sink.getClass().getSimpleName());
}
}

/** Returns an unmodifiable snapshot of all events that reached each topic. */
public List<String> subscribedTopics() {
return Collections.unmodifiableList(new ArrayList<>(subscriptions.keySet()));
}
}
