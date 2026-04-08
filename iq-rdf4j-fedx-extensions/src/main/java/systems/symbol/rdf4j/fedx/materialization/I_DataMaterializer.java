package systems.symbol.rdf4j.fedx.materialization;

import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;

import systems.symbol.rdf4j.fedx.FedXEndpoint;

/**
 * Strategy interface for materializing data (not schema) from a source.
 *
 * <p>Always fetches live data directly from the source. No caching. Every query results in a
 * remote fetch. Suitable for frequently-changing data or scenarios requiring freshness.
 *
 * <p>Data materialization is kept separate from schema introspection ({@code I_SchemaMaterializer
 * }). This allows:
 * - Schema: cached with long TTL (rarely changes)
 * - Data: real-time fetches (always fresh)
 * - Decoupled cache strategies per concern
 *
 * @see I_SchemaMaterializer
 */
public interface I_DataMaterializer {

  /**
   * Execute a query against the source and return live results.
   *
   * <p>No caching. Every invocation fetches from the source. Direct delegation to remote SPARQL
   * client.
   *
   * @param endpoint the virtual graph endpoint with source metadata
   * @param sparqlQuery the SPARQL SELECT query fragment or full query
   * @return list of result bindings, never null but may be empty
   * @throws MaterializationException if remote fetch fails
   */
  List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery)
      throws MaterializationException;

  /**
   * Refresh state (metrics, latency cache, etc.).
   *
   * <p>Called on connector refresh cycle. For real-time materializer, this is typically a no-op,
   * but subclasses may track metrics or reset transient state.
   *
   * @param endpoint the virtual graph to refresh
   */
  void refresh(FedXEndpoint endpoint);

  /**
   * Get the materialization strategy name (for diagnostics/metrics).
   *
   * @return strategy name (e.g., "REALTIME", "WRITE_THROUGH")
   */
  String strategy();
}
