package systems.symbol.mcp;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;
import java.util.Optional;

/**
 * MCP Tool Execution Result interface.
 * 
 * Encapsulates the outcome of invoking a tool via MCP adapter:
 * - success/failure status
 * - payload (RDF Model or JSON)
 * - audit trail (who, when, why, cost)
 * - error details (if applicable)
 * 
 * Results are typically stored in the realm's audit log as RDF triples.
 */
public interface I_MCPResult {

/**
 * Returns true if tool execution succeeded.
 * 
 * @return success status
 */
boolean isSuccess();

/**
 * Retrieves the tool's result payload as an RDF Model.
 * 
 * @return RDF Model (empty model if tool produced no RDF output)
 */
Model getPayload();

/**
 * Retrieves error message if execution failed.
 * 
 * @return Optional error message
 */
Optional<IRI> getError();

/**
 * Retrieves the exception (if execution failed with exception).
 * 
 * @return Optional exception
 */
Optional<Throwable> getCause();

/**
 * Retrieves the execution audit trail as RDF triples.
 * Audit model should include:
 * - iq:AgentLog predicate describing who invoked the tool
 * - prov:startTime, prov:endTime for duration tracking
 * - iq:cost for resource consumption
 * - iq:signature for non-repudiation
 * 
 * @return RDF Model containing audit log triples
 */
Model getAudit();

/**
 * Retrieves the estimated cost (tokens, API calls, compute units).
 * 
 * @return cost as int
 */
int getCost();

/**
 * Retrieves the execution duration in milliseconds.
 * 
 * @return duration
 */
long getDurationMillis();
}
