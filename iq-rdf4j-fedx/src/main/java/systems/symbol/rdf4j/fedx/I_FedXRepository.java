package systems.symbol.rdf4j.fedx;

import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * High-level API for federated SPARQL queries.
 * Orchestrates query execution across federation topology.
 *
 * Usage:
 *   I_FedXRepository fedx = new FedXRepository(topology, client, optimizer);
 *   TupleQueryResult result = fedx.prepareTupleQuery("SELECT ?s WHERE { ?s ?p ?o }").evaluate();
 */
public interface I_FedXRepository {

/**
 * Prepares a tuple query (SELECT) for federated execution.
 *
 * @param query the SPARQL query string
 * @return prepared query ready to evaluate
 * @throws RepositoryException if query parsing fails
 */
TupleQuery prepareTupleQuery(String query) throws RepositoryException;

/**
 * Prepares a graph query (CONSTRUCT) for federated execution.
 *
 * @param query the SPARQL query string
 * @return prepared query ready to evaluate
 * @throws RepositoryException if query parsing fails
 */
GraphQuery prepareGraphQuery(String query) throws RepositoryException;

/**
 * Prepares a boolean query (ASK) for federated execution.
 *
 * @param query the SPARQL query string
 * @return prepared query ready to evaluate
 * @throws RepositoryException if query parsing fails
 */
BooleanQuery prepareBooleanQuery(String query) throws RepositoryException;

/**
 * Gets the current federation topology.
 */
I_FedXTopology getTopology();

/**
 * Gets the federated query optimizer.
 */
I_FederatedQueryOptimizer getOptimizer();

/**
 * Refreshes the federation topology (useful for dynamic endpoint discovery).
 */
void refreshTopology();

/**
 * Closes the repository and releases resources.
 */
void close() throws RepositoryException;
}
