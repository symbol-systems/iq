package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.KernelBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KernelEventHubTest {

@Test
void kernelTopicsAreConstantIris() {
assertNotNull(KernelTopics.KERNEL_START);
assertEquals("urn:iq:event:kernel:start", KernelTopics.KERNEL_START.stringValue());
assertEquals("urn:iq:event:rdf:statement:add", KernelTopics.RDF_STATEMENT_ADD.stringValue());
}

@Test
void noopEventHubDoesNothingAndDoesNotThrow() {
I_EventHub hub = new NoopEventHub();
KernelEvent event = KernelEvent.on(KernelTopics.CLI_COMMAND_START)
.text("no-op payload")
.build();

assertDoesNotThrow(() -> hub.publish(event));

I_EventSink sink = e -> fail("NoopEventHub should not call sink");
assertDoesNotThrow(() -> hub.subscribe(KernelTopics.CLI_COMMAND_START, sink));
assertDoesNotThrow(() -> hub.unsubscribe(KernelTopics.CLI_COMMAND_START, sink));
}

@Test
void simpleEventHubDeliversToSubscribedSink() throws KernelException {
SimpleEventHub hub = new SimpleEventHub();
List<String> received = new ArrayList<>();

I_EventSink sink = event -> received.add(event.getPayload(String.class));
hub.subscribe(KernelTopics.SCRIPT_EXECUTE_PRE, sink);

KernelEvent event = KernelEvent.on(KernelTopics.SCRIPT_EXECUTE_PRE)
.text("hello")
.build();

hub.publish(event);

assertEquals(1, received.size());
assertEquals("hello", received.get(0));
}

@Test
void safeEventHubSwallowsSinkExceptions() throws KernelException {
SimpleEventHub inner = new SimpleEventHub();
SafeEventHub safe = new SafeEventHub(inner);
List<String> delivered = new ArrayList<>();

I_EventSink bad = event -> { throw new KernelException("kernel.event", "failed"); };
I_EventSink good = event -> delivered.add(event.getPayload(String.class));

safe.subscribe(KernelTopics.CLI_COMMAND_START, bad);
safe.subscribe(KernelTopics.CLI_COMMAND_START, good);

KernelEvent event = KernelEvent.on(KernelTopics.CLI_COMMAND_START)
.text("payload")
.build();

// safe publish should not propagate exceptions
assertDoesNotThrow(() -> safe.publish(event));

// good sink should still receive even when sibling throws
assertEquals(1, delivered.size(), "SafeEventHub should allow non-failing sinks to run");
assertEquals("payload", delivered.get(0));
}

@Test
void kernelBuilderWithEventHubIsAccessibleFromContext() throws Exception {
SimpleEventHub hub = new SimpleEventHub();
var kernel = KernelBuilder.create()
.withHome(new java.io.File("target/iq-kernel-test"))
.withEventHub(hub)
.build();

kernel.start();
assertSame(hub, kernel.getContext().getEventHub());

kernel.stop();
}
}
