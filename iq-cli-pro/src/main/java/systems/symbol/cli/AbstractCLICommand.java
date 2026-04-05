package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;
import systems.symbol.kernel.command.KernelRequest;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Base class for IQ CLI Pro commands with kernel context support.
 * 
 * Extends the core CLI framework with:
 * - Auth context (JWT token validation, principal extraction)
 * - Audit logging (mutations to mcp:audit graph)
 * - Quota enforcement (rate limiting per principal)
 * 
 * All commands execute within a KernelContext that enforces:
 * 1. Authentication (JWT tokens in ~/.iq/tokens/)
 * 2. Authorization (realm-based access control)  
 * 3. Audit trail (all mutations logged)
 * 4. Rate limits (configurable quota per principal per hour)
 * 
 * Usage:
 *   public class MyCommand extends AbstractCLICommand {
 *   @Override
 *   public Object call() {
 *   // Automatically validated by kernel before execution
 *   // Audit logged after execution
 *   // No quota checks needed (automatic)
 *   }
 *   }
 * 
 * @author Symbol Systems
 * @version 0.94+auth
 */
public abstract class AbstractCLICommand extends systems.symbol.kernel.command.AbstractKernelCommand<Object>
implements Callable<Object> {

protected static final Logger log = LoggerFactory.getLogger(AbstractCLICommand.class);

protected final CLIContext context;

/**
 * Initialize command with CLI context (which holds KernelContext).
 * 
 * The kernel context is automatically configured with:
 * - Auth middleware: verifies JWT token from ~/.iq/tokens/<realm>.jwt
 * - Audit middleware: logs all mutations to mcp:audit graph
 * - Quota middleware: tracks command frequency per principal
 * 
 * @param context CLI context with realm, IQStore, and display
 * @throws IOException if unable to access workspace
 */
public AbstractCLICommand(CLIContext context) throws IOException {
super(context.getKernelContext());
this.context = context;
}

/**
 * Get the KernelContext from this command's context.
 * 
 * The kernel context provides:
 * - principal: authenticated user/service  
 * - realm: current IQ realm
 * - audit: audit trail for mutations
 * - quota: rate limiting tracker
 * 
 * @return KernelContext for this command execution
 */
public KernelContext getKernelContext() {
return context.getKernelContext();
}

/**
 * Create a KernelRequest for domain operations.
 * Automatically includes:
 * - subject: the caller's IRI (realm actor)
 * - realm: the current realm
 * - parameters for operation context
 * 
 * Example:
 *   KernelRequest req = kernelRequest(context.getSelf())
 *   .param("action", "create-model")
 *   .build();
 *   // Kernel automatically validates auth + audit + quota
 * 
 * @param subject IRI of the subject performing the action
 * @return builder for KernelRequest
 */
public KernelRequest.Builder kernelRequest(String subject) {
return KernelRequest.on(context.getSelf())
.realm(context.getSelf())
.param("subject", subject)
.param("caller", context.getSelf());
}

/**
 * Display output to user.
 * 
 * @param msg message to display
 */
public void display(String msg) {
context.getDisplay().out(msg);
}

/**
 * Display error message to user.
 * 
 * @param msg error message
 */
public void displayError(String msg) {
context.getDisplay().err(msg);
}

/**
 * Display formatted output to user.
 * 
 * @param format format string (Java String.format syntax)
 * @param args format arguments
 */
public void displayf(String format, Object... args) {
context.getDisplay().out(String.format(format, args));
}

/**
 * Display formatted error message to user.
 * 
 * @param format format string
 * @param args format arguments
 */
public void displayErrorf(String format, Object... args) {
context.getDisplay().err(String.format(format, args));
}

/**
 * Display blank line to user.
 */
public void display() {
context.getDisplay().out("");
}

/**
 * Execute command with kernel context validation.
 * 
 * The kernel automatically:
 * 1. Validates auth (JWT token in ~/.iq/tokens/)
 * 2. Extracts principal (user ID from token)
 * 3. Enforces realm isolation (SPARQL FILTER)
 * 4. Logs command execution to mcp:audit
 * 5. Tracks quota usage (rate limits)
 * 6. Returns 429 if quota exceeded
 * 
 * Subclasses override doCall(), not this method.
 * 
 * @return command output (or error result)
 * @throws Exception if command fails
 */
@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
throw new CLIException("IQ workspace not initialized");
}

KernelRequest request = kernelRequest(context.getSelf().stringValue()).build();
systems.symbol.kernel.command.KernelResult<Object> result = execute(request);
return result.isSuccess() ? result.value().orElse(null) : result.failure().get();
}

/**
 * Internal: execute kernel request with auth/audit/quota middleware.
 * Do not override; subclasses implement doCall() instead.
 * 
 * @param request KernelRequest with context
 * @return result with value or failure
 */
@Override
protected systems.symbol.kernel.command.KernelResult<Object> doExecute(KernelRequest request) {
try {
Object output = doCall();
return systems.symbol.kernel.command.KernelResult.ok(output);
} catch (systems.symbol.kernel.KernelException ke) {
throw ke;
} catch (Exception e) {
throw new systems.symbol.kernel.KernelCommandException("cli.command.failed", e.getMessage(), e);
}
}

/**
 * Subclasses override this to implement command logic.
 * 
 * This method is called after kernel validation, within:
 * - Auth context: principal is authenticated and available via getKernelContext()
 * - Audit logging: all mutations are tracked in mcp:audit graph
 * - Quota enforcement: rate limits are checked before and after
 * 
 * Example:
 *   @Override
 *   protected Object doCall() throws Exception {
 *   // Access authenticated principal
 *   IRI principal = getKernelContext().getPrincipal();
 *   // All queries now include realm FILTER
 *   // All mutations are audit logged
 *   // All commands respect rate limits
 *   
 *   IQStore iq = context.newIQBase();
 *   // ... command logic ...
 *   return result;
 *   }
 * 
 * @return command result
 * @throws Exception if command execution fails
 */
protected Object doCall() throws Exception {
return null;
}
}
