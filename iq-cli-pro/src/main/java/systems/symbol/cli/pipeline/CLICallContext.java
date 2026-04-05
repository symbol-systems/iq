package systems.symbol.cli.pipeline;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.pipeline.KernelCallContext;

/**
 * CLI-specific call context extending the kernel pipeline's base context.
 *
 * <p>Adds CLI-specific fields:
 * - commandName: the Picocli command being executed (e.g., "trigger", "boot")
 * - commandArgs: the arguments passed to the command
 * - executorIRI: the IRI of the CLI user/principal
 *
 * <p>Example usage:
 * <pre>{@code
 * CLICallContext ctx = new CLICallContext("trigger", args, userIRI);
 * ctx.set("someKey", someValue);  // Common pipeline attributes
 * pipeline.process(ctx);
 * }</pre>
 *
 * @author Symbol Systems
 */
public final class CLICallContext extends KernelCallContext {

private final String commandName;
private final String[] commandArgs;
private final IRI executorIRI;

/**
 * Create a new CLI call context.
 *
 * @param commandName  The Picocli command name (e.g., "trigger")
 * @param commandArgs  The arguments passed to the command
 * @param executorIRI  The IRI of the user executing the command
 */
public CLICallContext(String commandName, String[] commandArgs, IRI executorIRI) {
super();
this.commandName = commandName;
this.commandArgs = commandArgs;
this.executorIRI = executorIRI;
}

public String commandName() {
return commandName;
}

public String[] commandArgs() {
return commandArgs;
}

public IRI executorIRI() {
return executorIRI;
}

@Override
public String toString() {
return "CLICallContext[" +
"trace=" + traceId() +
", command=" + commandName +
", executor=" + executorIRI +
", auth=" + isAuthorised() +
"]";
}
}
