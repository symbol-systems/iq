package systems.symbol.kernel;

import systems.symbol.platform.I_StartStop;
import systems.symbol.platform.I_Self;

/**
 * Contract for the IQ kernel lifecycle.
 *
 * <p>Each surface (REST, MCP, CLI, Camel) obtains one {@code I_Kernel} instance
 * via {@link KernelBuilder} and drives its lifecycle via {@code start()} / {@code stop()}.
 * The surface may then retrieve the {@link KernelContext} to wire its own
 * controllers, tools, or route processors.
 */
public interface I_Kernel extends I_StartStop, I_Self {

/**
 * Returns the fully-initialised context.
 *
 * <p>Valid only after {@link #start()} has returned successfully.
 *
 * @throws KernelBootException if called before {@code start()}
 */
KernelContext getContext();
}
