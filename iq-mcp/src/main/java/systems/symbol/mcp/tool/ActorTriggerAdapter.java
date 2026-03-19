package systems.symbol.mcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.util.List;
import java.util.Map;

/**
 * ActorTriggerAdapter — Pillar C: the agentic bridge tool.
 *
 * <p>Publishes an {@code iq:Intent} into the IQ agentic bus.
 * The intent is identified by a URI and an optional set of parameter bindings.
 * The actual execution is delegated to the provided {@link I_IntentDispatcher}
 * functional interface, which in production wires to
 * {@code IntentAPI} / {@code AgentService}.
 *
 * <p>Tool name: {@value systems.symbol.mcp.MCP_NS#TOOL_ACTOR_TRIGGER}
 */
public class ActorTriggerAdapter implements I_MCPTool {

    private static final Logger log = LoggerFactory.getLogger(ActorTriggerAdapter.class);

    /**
     * Functional interface for the agentic dispatch layer.
     * Replace the lambda with a CDI/Quarkus injection in production:
     * {@code (intentUri, params) -> intentAPI.trigger(realm, intentUri, params)}
     */
    @FunctionalInterface
    public interface I_IntentDispatcher {
        /**
         * Dispatch an intent.
         *
         * @param intentUri the intent IRI (e.g. {@code iq:SyncData})
         * @param params    key-value bindings for the intent
         * @return a JSON result string
         * @throws Exception on execution failure
         */
        String dispatch(String intentUri, Map<String, Object> params) throws Exception;
    }

    private final I_IntentDispatcher dispatcher;

    public ActorTriggerAdapter(I_IntentDispatcher dispatcher) { this.dispatcher = dispatcher; }

    /** Convenience: no-op dispatcher for testing. */
    public static ActorTriggerAdapter noOp() {
        return new ActorTriggerAdapter((uri, params) ->
                "{\"status\":\"triggered\",\"intent\":\"" + uri + "\"}");
    }

    @Override public String getName()       { return "actor.trigger"; }
    @Override public boolean isReadOnly()   { return false; }
    @Override public int defaultRateLimit() { return 20; }
    @Override public int order()            { return 40; }

    @Override
    public String getDescription() {
        return """
               Trigger an IQ Agent intent (iq:Intent / state-machine transition).
               Use this for actions with system-level side-effects that go beyond graph mutation:
               sending notifications, starting pipelines, invoking external APIs.
               
               Example intents: "iq:SyncData", "iq:SendAlert", "iq:ApproveDocument"
               
               Requires authentication. Rate-limited to 20 calls/minute.
               """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "intentUri", Map.of("type", "string", "description", "The IRI of the iq:Intent to trigger."),
                "actor",     Map.of("type", "string", "description", "The IRI of the actor performing the action."),
                "params",    Map.of("type", "object", "description", "Optional key-value bindings passed to the intent.")
            ),
            "required", List.of("intentUri")
        );
    }

    @Override
    public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
        String intentUri = (String) input.get("intentUri");
        if (intentUri == null || intentUri.isBlank()) throw MCPException.badRequest("'intentUri' is required");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = input.containsKey("params")
                ? (Map<String, Object>) input.get("params") : Map.of();

        log.info("[actor.trigger] principal={} intent={} [trace={}]", ctx.principal(), intentUri, ctx.traceId());

        try {
            String result = dispatcher.dispatch(intentUri, params);
            return MCPResult.okJson(result);
        } catch (MCPException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[actor.trigger] dispatch failed for intent={} [trace={}]", intentUri, ctx.traceId(), ex);
            throw MCPException.internal("Intent dispatch failed: " + ex.getMessage(), ex);
        }
    }
}
