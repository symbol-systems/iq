package systems.symbol.runtime;

import org.eclipse.rdf4j.model.IRI;

/**
 * Interface for coordinating process execution within the platform.
 * Implementations provided by iq-runtime module.
 */
public interface ProcessCoordinator {
/**
 * Submit a task for execution identified by IRI.
 */
void submit(IRI taskId) throws Exception;

/**
 * Wait for a specific task to complete.
 */
void await(IRI taskId, long timeoutMs) throws Exception;

/**
 * Check if a task is currently running.
 */
boolean isRunning(IRI taskId);

/**
 * Cancel an in-flight task.
 */
void cancel(IRI taskId) throws Exception;

/**
 * Get the result of a completed task (null if not finished).
 */
Object getResult(IRI taskId);
}
