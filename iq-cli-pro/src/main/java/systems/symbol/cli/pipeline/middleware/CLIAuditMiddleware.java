package systems.symbol.cli.pipeline.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.cli.pipeline.CLICallContext;
import systems.symbol.kernel.KernelException;
import systems.symbol.kernel.pipeline.I_Chain;
import systems.symbol.kernel.pipeline.I_Middleware;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * CLI Audit Middleware.
 */
public class CLIAuditMiddleware implements I_Middleware<CLICallContext> {
private static final Logger log = LoggerFactory.getLogger(CLIAuditMiddleware.class);

@Override
public int getOrder() {
return 200;
}

@Override
public void process(CLICallContext ctx, I_Chain<CLICallContext> chain) throws KernelException {
String commandName = ctx.commandName();
String principal = ctx.principal();
Instant startTime = ctx.startTime();

log.info("AUDIT: CLI command started: command={}, principal={}, trace={}", 
 commandName, principal, ctx.traceId());

try {
chain.proceed(ctx);
long durationMs = startTime.until(Instant.now(), ChronoUnit.MILLIS);
log.info("AUDIT: CLI command completed: command={}, principal={}, duration={}ms, trace={}", 
 commandName, principal, durationMs, ctx.traceId());
} catch (KernelException e) {
long durationMs = startTime.until(Instant.now(), ChronoUnit.MILLIS);
log.info("AUDIT: CLI command failed: command={}, principal={}, duration={}ms, error={}, trace={}", 
 commandName, principal, durationMs, e.getMessage(), ctx.traceId());
throw e;
}
}

@Override
public String displayName() {
return "CLIAuditMiddleware";
}
}
