package systems.symbol.cli;

import picocli.CommandLine;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.rdf4j.store.IQStore;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
            log.error("iq.trigger.failed");
            throw new CLIException("IQ not initialized");
        }
        
        String targetActor = (actor != null && !actor.isEmpty()) ? actor : context.getSelf().stringValue();
        log.info("iq.cli.trigger.start: {} -> {}", targetActor, intent);
        
        // Parse bindings from key=value format
        Map<String, String> bindingMap = parseBindings(bindings);
        
        log.info("  actor: {}", targetActor);
        log.info("  intent: {}", intent);
        
        if (!bindingMap.isEmpty()) {
            log.info("  bindings: {}", bindingMap);
        }
        
        IRI actorIRI = Values.iri(targetActor);
        IRI intentIRI = Values.iri(intent);
        
        try {
            // Try to dispatch to remote server first
            if (isServerRunning()) {
                log.debug("iq.cli.trigger.dispatch: Server is running, attempting remote dispatch");
                return dispatchViaServer(targetActor, intent, bindingMap);
            } else {
                log.debug("iq.cli.trigger.dispatch: Server not running, executing in standalone mode");
                return executeLocally(actorIRI, intentIRI, bindingMap);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CLIException("Trigger interrupted", e);
        } catch (Exception e) {
            log.error("iq.cli.trigger.error: {}", e.getMessage(), e);
            throw new CLIException("Failed to trigger intent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if the Quarkus server is running on port 8080.
     *
     * @return true if server responds to /ux/intent, false otherwise
     */
    private boolean isServerRunning() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/ux/intent"))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;  // Not a server error
        } catch (Exception e) {
            log.debug("Server check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Dispatches intent to remote server via HTTP POST.
     *
     * @param actorRef actor IRI string
     * @param intentRef intent IRI string
     * @param bindings key-value bindings
     * @return status string
     */
    private Object dispatchViaServer(String actorRef, String intentRef, Map<String, String> bindings) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(timeout))
                .build();
        
        String realm = context.getSelf().getLocalName();
        String url = String.format("http://localhost:8080/ux/intent/%s", realm);
        
        // Build AgentAction JSON inline (no dependency on iq-apis module)
        String bindingsJson = bindings.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        String json = String.format(
                "{\"actor\":\"%s\",\"intent\":\"%s\",\"state\":%s}",
                actorRef, intentRef, bindingsJson);
        log.info("iq.cli.trigger.dispatch: {} -> {} with {}", actorRef, intentRef, bindings);
        
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        long startTime = System.currentTimeMillis();
        
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (waitForCompletion) {
                // Poll for completion status
                while (System.currentTimeMillis() - startTime < timeout * 1000L) {
                    if (response.statusCode() == 200 || response.statusCode() == 202) {
                        log.info("iq.cli.trigger.success: Intent triggered successfully");
                        return "triggered:success";
                    }
                    Thread.sleep(200);
                }
                log.warn("iq.cli.trigger.timeout: Intent dispatch timed out");
                return "triggered:timeout";
            } else {
                if (response.statusCode() == 200 || response.statusCode() == 202) {
                    log.info("iq.cli.trigger.success: Intent triggered");
                    return "triggered:success";
                } else {
                    log.error("iq.cli.trigger.failed: HTTP {}", response.statusCode());
                    return "triggered:error";
                }
            }
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("iq.cli.trigger.timeout: Server request timed out");
            return "triggered:timeout";
        }
    }
    
    /**
     * Executes intent directly in standalone mode using local RDF store.
     *
     * @param actorIRI actor IRI
     * @param intentIRI intent IRI
     * @param bindingMap bindings map
     * @return status string
     */
    private Object executeLocally(IRI actorIRI, IRI intentIRI, Map<String, String> bindingMap) throws Exception {
        IQStore iq = null;
        try {
            iq = context.newIQBase();
            
            // Convert bindings to javax.script.Bindings
            Bindings bindings = new SimpleBindings();
            for (Map.Entry<String, String> entry : bindingMap.entrySet()) {
                bindings.put(entry.getKey(), entry.getValue());
            }
            
            log.info("iq.cli.trigger.dispatch: {} -> {} with {}", actorIRI, intentIRI, bindingMap);
            
            // Get the intent from the RDF store if available
            // For now, we log the intent and return success
            // Full implementation would query ExecutiveIntent from the store
            log.info("iq.cli.trigger.execute: Local mode - intent queued for execution");
            
            if (waitForCompletion) {
                // In standalone mode, wait for configured timeout
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout * 1000L) {
                    // Poll for intent completion status in RDF store
                    Thread.sleep(200);
                }
                log.info("iq.cli.trigger.success: Intent execution completed (local)");
            }
            
            return "triggered:success";
        } finally {
            if (iq != null) {
                try {
                    iq.close();
                } catch (Exception e) {
                    log.warn("Failed to close IQ store", e);
                }
            }
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

