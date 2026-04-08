package systems.symbol.rdf4j.fedx;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Strategy interface for executing SPARQL queries against a specific remote endpoint type.
 *
 * <p>
 * Each implementation handles one specific concern:
 * </p>
 * <ul>
 *   <li>{@link JdbcRemoteSPARQLClient} — translates SPARQL to SQL</li>
 *   <li>{@link OpenApiRemoteSPARQLClient} — translates SPARQL to REST API calls</li>
 *   <li>{@link GraphQLRemoteSPARQLClient} — translates SPARQL to GraphQL queries</li>
 *   <li>{@link HttpRemoteSPARQLClient} — standard SPARQL HTTP endpoint queries</li>
 * </ul>
 *
 * <p>
 * This follows the Strategy pattern: each executor encapsulates an algorithm for translating
 * SPARQL to a specific query language or protocol. The router ({@link RemoteSPARQLClientRouter})
 * dispatches to the appropriate strategy based on endpoint scheme.
 * </p>
 *
 * <p>
 * Benefits:
 * </p>
 * <ul>
 *   <li>✅ Each executor has <strong>one responsibility</strong> (single concern)</li>
 *   <li>✅ Easy to add new types (create executor, register in router)</li>
 *   <li>✅ Easy to test (mock each executor independently)</li>
 *   <li>✅ No god class, no god method</li>
 *   <li>✅ Follows Interface Segregation Principle</li>
 * </ul>
 */
public interface I_RemoteSPARQLExecutor {

  /**
   * Determine if this executor can handle the given endpoint.
   *
   * <p>
   * Implementation should check the endpoint's SPARQL endpoint URI scheme
   * and return true if this executor is the appropriate handler.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   * <ul>
   *   <li>JdbcRemoteSPARQLClient: <code>canHandle("urn:iq:sparql:jdbc:...")</code></li>
   *   <li>OpenApiRemoteSPARQLClient: <code>canHandle("urn:iq:sparql:openapi:...")</code></li>
   *   <li>GraphQLRemoteSPARQLClient: <code>canHandle("urn:iq:sparql:graphql:...")</code></li>
   *   <li>HttpRemoteSPARQLClient: <code>canHandle("http://...", "https://...", or fallback)</code></li>
   * </ul>
   *
   * @param endpoint The FedX endpoint to check
   * @return true if this executor can handle this endpoint
   */
  boolean canHandle(FedXEndpoint endpoint);

  /**
   * Execute a SPARQL query against the endpoint.
   *
   * <p>
   * Implementation translates SPARQL to the appropriate query language
   * (SQL, REST API, GraphQL, or HTTP SPARQL), executes, and returns results
   * as SPARQL bindings.
   * </p>
   *
   * @param endpoint The remote endpoint
   * @param sparqlQuery The SPARQL query string
   * @return Query results as SPARQL tuple bindings
   * @throws RepositoryException if query execution fails
   */
  TupleQueryResult execute(FedXEndpoint endpoint, String sparqlQuery)
      throws RepositoryException;
}
