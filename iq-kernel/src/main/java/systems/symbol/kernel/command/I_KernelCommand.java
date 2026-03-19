package systems.symbol.kernel.command;

/**
 * Transport-agnostic unit of executable work.
 *
 * <p>Analogous to CLI's {@code AbstractCLICommand}, REST's {@code RealmAPI}
 * handler, MCP's {@code I_MCPTool}, and Camel's {@code AbstractCamelRDFProcessor}.
 * All four surface types may delegate their core logic to an implementation of
 * this interface, with surface-specific code only at the boundary.
 *
 * @param <T> the result payload type
 */
public interface I_KernelCommand<T> {

/**
 * Execute the command.
 *
 * @param request the transport-agnostic request
 * @return a discriminated union of success or failure
 */
KernelResult<T> execute(KernelRequest request);
}
