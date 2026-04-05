package systems.symbol.cli.pipeline.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.cli.pipeline.CLICallContext;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;
import systems.symbol.kernel.pipeline.KernelCallContext;

/**
 * CLI Auth Middleware.
 */
public class CLIAuthMiddleware implements I_Middleware<CLICallContext> {
private static final Logger log = LoggerFactory.getLogger(CLIAuthMiddleware.class);

@Override
public int getOrder() {
return 100;
}

@Override
public void process(CLICallContext ctx, I_Chain<CLICallContext> chain) throws KernelException {
log.debug("Auth middleware processing: {}", ctx.commandName());
ctx.set(KernelCallContext.KEY_PRINCIPAL, ctx.executorIRI().stringValue());
ctx.set(KernelCallContext.KEY_AUTHORISED, true);
log.debug("Auth: CLI user authenticated as {}", ctx.executorIRI());
chain.proceed(ctx);
}

@Override
public String displayName() {
return "CLIAuthMiddleware";
}
}
