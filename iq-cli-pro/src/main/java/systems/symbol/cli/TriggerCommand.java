package systems.symbol.cli;

import picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
System.out.println("iq.trigger.failed");
throw new CLIException("IQ not initialized");
}

String targetActor = (actor != null && !actor.isEmpty()) ? actor : context.getSelf().stringValue();
log.info("iq.cli.trigger.start: {} -> {}", targetActor, intent);

// Parse bindings from key=value format
Map<String, String> bindingMap = parseBindings(bindings);

System.out.println("  actor: " + targetActor);
System.out.println("  intent: " + intent);

if (!bindingMap.isEmpty()) {
System.out.println("  bindings: " + bindingMap);
}

try {
// TODO: Wire up Apache Camel integration for event routing
// Current implementation is a stub; production implementation should:
// 1. If server mode (iq-apis running): POST to /ux/intent/{realm}/trigger via IntentAPI
// 2. If standalone: Execute intent directly via I_Intent interface
// 3. Handle async execution and status polling if --wait flag is set

// For now, just simulate the intent execution with logging
if (waitForCompletion) {
System.out.println("  waiting for completion (timeout: " + timeout + "s)...");
// Simulate async execution with timeout
long startTime = System.currentTimeMillis();
long timeoutMs = timeout * 1000L;

while (System.currentTimeMillis() - startTime < timeoutMs) {
// Check for completion status (would poll Camel route or IntentAPI)
Thread.sleep(100);
break; // For stub, exit immediately
}
System.out.println("  completed (stub implementation)");
}

System.out.println("iq.cli.trigger.done: event fired");
log.info("iq.cli.trigger.done: {} fired (stub)", intent);
return "triggered";
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
throw new CLIException("Trigger interrupted", e);
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

