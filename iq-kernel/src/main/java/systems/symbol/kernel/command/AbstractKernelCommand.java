package systems.symbol.kernel.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;
import systems.symbol.kernel.KernelCommandException;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.event.KernelTopics;

/**
 * Base class for kernel commands.
 *
 * <p>Wires a {@link KernelContext} and provides structured error handling.
 * Surface adapters obtain a {@code KernelContext} from their surface-level
 * lifecycle singleton and pass it at construction time.
 *
 * <p>Minimal example:
 * <pre>{@code
 * public class SparqlSelectCommand extends AbstractKernelCommand<List<BindingSet>> {
 *     public SparqlSelectCommand(KernelContext ctx) { super(ctx); }
 *
 *     @Override
 *     protected KernelResult<List<BindingSet>> doExecute(KernelRequest request)
 *             throws KernelException {
 *         String query = (String) request.getParams().get("query");
 *         // … use ctx.getSecrets(), context.getHome(), etc.
 *         return KernelResult.ok(results);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the result payload type
 */
public abstract class AbstractKernelCommand<T> implements I_KernelCommand<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final KernelContext ctx;

    protected AbstractKernelCommand(KernelContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Implement the core command logic here.
     * Checked exceptions from the domain layer should be wrapped in a
     * {@link KernelException} subtype before being thrown.
     */
    protected abstract KernelResult<T> doExecute(KernelRequest request) throws KernelException;

    @Override
    public final KernelResult<T> execute(KernelRequest request) {
        log.debug("kernel.command.execute: {} -> {}", getClass().getSimpleName(), request);

        systems.symbol.kernel.event.I_EventHub hub = ctx.getEventHub();
        publishCommandEvent(hub, KernelTopics.AGENT_COMMAND_RECEIVED, request, null);

        try {
            KernelResult<T> result = doExecute(request);
            publishCommandEvent(hub, KernelTopics.AGENT_COMMAND_EXECUTED, request, null);
            return result;

        } catch (KernelException ke) {
            log.warn("kernel.command.error: {} -> {}", getClass().getSimpleName(), ke.getCode());
            publishCommandEvent(hub, KernelTopics.AGENT_COMMAND_FAILED, request, ke);
            return KernelResult.error(ke);
        } catch (Exception e) {
            log.error("kernel.command.fatal: {}", getClass().getSimpleName(), e);
            KernelCommandException ex = new KernelCommandException(
                    "kernel.command.unexpected", e.getMessage(), e);
            publishCommandEvent(hub, KernelTopics.AGENT_COMMAND_FAILED, request, ex);
            return KernelResult.error(ex);
        }
    }

    private void publishCommandEvent(systems.symbol.kernel.event.I_EventHub hub,
                                     org.eclipse.rdf4j.model.IRI topic,
                                     KernelRequest request,
                                     Throwable problem) {
        if (hub == null) {
            return;
        }

        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"subject\":\"").append(request.getSubject()).append("\"");
        if (request.getCaller() != null) {
            payload.append(",\"caller\":\"").append(request.getCaller()).append("\"");
        }
        if (request.getRealm() != null) {
            payload.append(",\"realm\":\"").append(request.getRealm()).append("\"");
        }
        if (request.getCommand() != null) {
            systems.symbol.agent.I_Command cmd = request.getCommand();
            payload.append(",\"cmd_actor\":\"").append(cmd.getActor()).append("\"");
            payload.append(",\"cmd_action\":\"").append(cmd.getAction()).append("\"");
            payload.append(",\"cmd_target\":\"").append(cmd.getTarget()).append("\"");
        }
        if (problem != null) {
            payload.append(",\"error\":\"").append(problem.getMessage().replace("\"", "\\\"")).append("\"");
        }
        payload.append('}');

        try {
            hub.publish(systems.symbol.kernel.event.KernelEvent.on(topic)
                    .source(ctx.getSelf())
                    .contentType("application/json")
                    .payload(payload.toString())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish command event {}: {}", topic, e.getMessage(), e);
        }
    }
}
