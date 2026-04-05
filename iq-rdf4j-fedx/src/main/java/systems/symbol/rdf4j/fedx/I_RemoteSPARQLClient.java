package systems.symbol.rdf4j.fedx;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.util.List;

/**
 * SPI for querying remote SPARQL endpoints.
 * Abstracts HTTP communication and result parsing.
 *
 * Implementations:
 * - HTTPRemoteSPARQLClient: HTTP-based remote SPARQL queries
 * - CachedRemoteSPARQLClient: Caches results with TTL
 */
public interface I_RemoteSPARQLClient {

/**
 * Executes a SELECT query against a remote SPARQL endpoint.
 *
 * @param endpoint the remote endpoint
 * @param query the SPARQL query
 * @param language query language (SPARQL, etc.)
 * @return list of binding sets (solutions)
 * @throws RepositoryException if query fails
 */
List<BindingSet> selectQuery(FedXEndpoint endpoint, String query, QueryLanguage language)
throws RepositoryException;

/**
 * Executes a ASK query (boolean result) against a remote endpoint.
 *
 * @param endpoint the remote endpoint
 * @param query the SPARQL query
 * @param language query language
 * @return true if query matches any results, false otherwise
 * @throws RepositoryException if query fails
 */
boolean askQuery(FedXEndpoint endpoint, String query, QueryLanguage language)
throws RepositoryException;

/**
 * Checks if an endpoint is reachable (liveness probe).
 *
 * @param endpoint the endpoint to check
 * @return true if endpoint is accessible, false otherwise
 */
boolean isEndpointReachable(FedXEndpoint endpoint);

/**
 * Gets the HTTP response time (in milliseconds) for a test query.
 * Used for query optimization (prefer fast endpoints).
 *
 * @param endpoint the endpoint to probe
 * @return response time in ms, or -1 if unreachable
 */
long getEndpointLatency(FedXEndpoint endpoint);
}
