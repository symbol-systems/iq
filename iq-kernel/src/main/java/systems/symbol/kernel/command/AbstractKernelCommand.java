package systems.symbol.kernel.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;
import systems.symbol.kernel.KernelCommandException;
import systems.symbol.kernel.KernelException;

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
        try {
            return doExecute(request);
        } catch (KernelException ke) {
            log.warn("kernel.command.error: {} -> {}", getClass().getSimpleName(), ke.getCode());
            return KernelResult.error(ke);
        } catch (Exception e) {
            log.error("kernel.command.fatal: {}", getClass().getSimpleName(), e);
            return KernelResult.error(
                    new KernelCommandException(
                            "kernel.command.unexpected", e.getMessage(), e));
        }
    }
}
