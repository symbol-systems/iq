package systems.symbol.platform;

/**
 * The {@code I_StartStop} interface defines methods for starting and stopping operations within a system.
 * Implementations of this interface manage the initialization and termination of resources and processes.
 */
public interface I_StartStop {

/**
 * Starts the operation of the system.
 *
 * @throws Exception if an error occurs during the startup process.
 */
void start() throws Exception;

/**
 * Stops the operation of the system.
 */
void stop();
}
