package systems.symbol.rdf4j.fedx;

/**
 * SPI for optimizing federated SPARQL queries.
 * Responsible for:
 * 1. Analyzing query to identify which endpoints can answer which parts
 * 2. Planning optimal join order across endpoints
 * 3. Pushing down filters and projections to remote endpoints
 *
 * Implementations:
 * - SimpleFederatedQueryOptimizer: basic endpoint selection
 * - AdvancedFederatedQueryOptimizer: cardinality-based optimization (future)
 */
public interface I_FederatedQueryOptimizer {

/**
 * Analyzes a SPARQL query and optimizes it for federated execution.
 * Returns the optimized query string or a plan for distributed execution.
 *
 * @param sparqlQuery the original SPARQL query string
 * @param topology the federation topology (available endpoints)
 * @param client the remote SPARQL client
 * @return optimized query string or execution plan
 */
String optimizeQueryForFederation(String sparqlQuery, I_FedXTopology topology, 
  I_RemoteSPARQLClient client);

/**
 * Gets the name/identifier of this optimizer for logging/debugging.
 */
String getOptimizerName();
}
