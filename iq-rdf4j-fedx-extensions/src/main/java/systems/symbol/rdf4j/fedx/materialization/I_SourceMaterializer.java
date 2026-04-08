package systems.symbol.rdf4j.fedx.materialization;

import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;

import systems.symbol.rdf4j.fedx.FedXEndpoint;

/**
 * Strategy interface for materializing data from a source (real-time vs cached).
 *
 * <p>Separates schema (always real-time) from data (configurable strategy). Schema
 * introspection happens via {@code I_SourceIntrospector}, while data materialization uses this
 * interface.
 */
public interface I_SourceMaterializer {

  /**
   * Execute a query against the source and return materialized results.
   *
   * <p>Implementation determines whether to:
   * - Fetch directly from source (real-time)
   * - Return cached results (if valid)
   * - Combine both (write-through)
   *
   * @param endpoint the virtual graph endpoint with source metadata
   * @param sparqlQuery the SPARQL SELECT query fragment (e.g., templated variable binding)
   * @return list of result bindings, never null but may be empty
   * @throws MaterializationException if source fetch or caching fails
   */
  List<BindingSet> materialize(FedXEndpoint endpoint, String sparqlQuery)
      throws MaterializationException;

  /**
   * Explicitly refresh the materializer's state (clear cache, reset metrics).
   *
   * <p>Called on connector refresh cycle to ensure cache invalidation is coordinated with
   * connector lifecycle.
   *
   * @param endpoint the virtual graph to refresh
   */
  void refresh(FedXEndpoint endpoint);

  /**
   * Check if the materializer has valid cached data for a given endpoint.
   *
   * <p>Used by query optimizer to decide if results can be served from cache or require a
   * remote fetch.
   *
   * @param endpoint the virtual graph to check
   * @return true if cached data exists and is still valid (not expired)
   */
  boolean hasCachedData(FedXEndpoint endpoint);

  /**
   * Clear all cached data for the given endpoint.
   *
   * <p>Called when cache is explicitly invalidated (e.g., source modified, or on connector
   * sync).
   *
   * @param endpoint the virtual graph to clear
   */
  void clearCache(FedXEndpoint endpoint);

  /**
   * Clear all cached data across all endpoints managed by this materializer.
   *
   * <p>Called on application shutdown or when cache strategy changes at runtime.
   */
  void clearAllCaches();

  /**
   * Get the materialization strategy name (for diagnostics/metrics).
   *
   * <p>Examples: "REALTIME", "CACHED", "WRITE_THROUGH".
   *
   * @return strategy name
   */
  String strategy();
}
