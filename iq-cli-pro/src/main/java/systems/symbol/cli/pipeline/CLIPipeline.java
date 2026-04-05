package systems.symbol.cli.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.cli.pipeline.middleware.CLIAuditMiddleware;
import systems.symbol.cli.pipeline.middleware.CLIAuthMiddleware;
import systems.symbol.cli.pipeline.middleware.CLIQuotaMiddleware;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;
import systems.symbol.kernel.pipeline.I_Pipeline;

import java.util.List;

/**
 * CLI Pipeline — Ordered chain-of-responsibility for CLI command execution.
 *
 * <p>Middleware execution order:
 * 1. Auth (order 100) — Verify caller identity
 * 2. Audit (order 200) — Log command execution
 * 3. Quota (order 300) — Enforce rate limits
 *
 * <p>Example usage:
 * <pre>{@code
 * CLIPipeline pipeline = new CLIPipeline();
 * CLICallContext ctx = new CLICallContext("trigger", args, userIRI);
 * I_Chain<CLICallContext> chain = pipeline.build(middleware);
 * chain.proceed(ctx);  // Runs through all middleware
 * }</pre>
 *
 * @author Symbol Systems
 */
public class CLIPipeline implements I_Pipeline<CLICallContext> {
private static final Logger log = LoggerFactory.getLogger(CLIPipeline.class);

private static final List<I_Middleware<CLICallContext>> DEFAULT_MIDDLEWARE = List.of(
new CLIAuthMiddleware(),
new CLIAuditMiddleware(),
new CLIQuotaMiddleware()
);

@Override
public I_Chain<CLICallContext> build(List<I_Middleware<CLICallContext>> middleware) {
log.info("Building CLI pipeline with {} middleware", middleware.size());
return I_Pipeline.of(middleware);
}

/**
 * Get the default CLI middleware chain.
 *
 * @return A chain with auth → audit → quota middleware
 */
public I_Chain<CLICallContext> getDefaultChain() {
return build(DEFAULT_MIDDLEWARE);
}
}
