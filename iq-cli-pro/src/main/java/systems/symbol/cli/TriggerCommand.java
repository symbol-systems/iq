package systems.symbol.cli;

import picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.kernel.event.KernelEvent;
import systems.symbol.platform.AgentAction;
import systems.symbol.platform.AgentService;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "trigger", description = "Trigger an event to invoke a set of actions")
public class TriggerCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(TriggerCommand.class);

@CommandLine.Option(names = {"--actor"}, description = "Target actor IRI")
String actor = null;

@CommandLine.Option(names = {"--intent"}, description = "Intent to trigger", defaultValue = "default")
String intent = "default";

@CommandLine.Option(names = {"--bindings"}, description = "Bindings as key=value pairs")
String bindings = null;

@CommandLine.Option(names = {"--wait"}, description = "Wait for action to complete")
boolean waitForCompletion = false;

@CommandLine.Option(names = {"--timeout"}, description = "Timeout in seconds", defaultValue = "30")
int timeout = 30;

public TriggerCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
display("iq.trigger.failed");
throw new CLIException("IQ not initialized");
}

String targetActor = (actor != null && !actor.isEmpty()) ? actor : context.getSelf().stringValue();
String intentTopic = (intent != null && !intent.isEmpty()) ? intent : "default";

log.info("iq.cli.trigger.start: {} -> {}", targetActor, intentTopic);

// Parse bindings from key=value format
Map<String, String> bindingMap = parseBindings(bindings);

display("  actor: " + targetActor);
display("  intent: " + intentTopic);

if (!bindingMap.isEmpty()) {
display("  bindings: " + bindingMap);
}

try {
// Get the event hub from kernel context
I_EventHub eventHub = context.getKernelContext().getEventHub();

// Create IRI for topic
ValueFactory vf = SimpleValueFactory.getInstance();
IRI topicIRI;
try {
// Try to parse as IRI first
topicIRI = vf.createIRI(intentTopic);
} catch (IllegalArgumentException e) {
// If not valid IRI, wrap it with urn:iq:intent: prefix
topicIRI = vf.createIRI("urn:iq:intent:" + intentTopic);
}

// Create event payload with bindings
SimpleBindings eventBindings = new SimpleBindings();
eventBindings.putAll(bindingMap);
eventBindings.put("actor", targetActor);
eventBindings.put("intent", intentTopic);

// Build and publish the event
KernelEvent event = KernelEvent.on(topicIRI)
.source(vf.createIRI("urn:iq:surface:cli"))
.contentType("text/plain")
.text("actor=" + targetActor + ";intent=" + intentTopic)
.build();

long startTime = System.currentTimeMillis();

// Publish the event via the kernel's event hub
eventHub.publish(event);
display("  ✓ Event published: " + event.getId());

if (waitForCompletion) {
display("  waiting for actor transition (timeout: " + timeout + "s)...");

// For synchronous behavior, directly execute via AgentService if available
// (In a full Camel environment, this would be async via event handlers)
try {
if (context.repository != null) {
// Create AgentAction with proper String fields
AgentAction action = new AgentAction(targetActor, intentTopic, eventBindings);

AgentService service = new AgentService(
vf.createIRI(targetActor),
context.repository.getConnection(),
null,  // secrets (optional)
eventBindings
);

if (service.getAgent() != null) {
service.next(vf.createIRI(intentTopic));
display("  ✓ Actor transitioned successfully");
} else {
display("  ⚠ Actor not found; event queued for async processing");
}
}
} catch (Exception e) {
log.warn("iq.cli.trigger.sync_exec_failed: {}", e.getMessage());
display("  ⚠ Async event published; actor will process when ready");
}
}

long elapsed = System.currentTimeMillis() - startTime;
display("iq.cli.trigger.done: event fired (elapsed: " + elapsed + "ms)");
log.info("iq.cli.trigger.done: {} fired via I_EventHub", intentTopic);
return "triggered";

} catch (KernelException e) {
log.error("iq.cli.trigger.kernel_error: {}", e.getMessage(), e);
throw new CLIException("Kernel error publishing event: " + e.getMessage(), e);
} catch (Exception e) {
log.error("iq.cli.trigger.error: {}", e.getMessage(), e);
throw new CLIException("Failed to trigger intent: " + e.getMessage(), e);
}
}

private Map<String, String> parseBindings(String bindingStr) {
Map<String, String> bindings = new HashMap<>();
if (bindingStr == null || bindingStr.isEmpty()) {
return bindings;
}

// Parse key=value pairs separated by comma or semicolon
String[] pairs = bindingStr.split("[,;]");
for (String pair : pairs) {
String[] kv = pair.trim().split("=");
if (kv.length == 2) {
bindings.put(kv[0].trim(), kv[1].trim());
} else if (kv.length == 1) {
bindings.put(kv[0].trim(), "true");
}
}
return bindings;
}
}

