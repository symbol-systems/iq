package systems.symbol.rdf4j.fedx.materialization;

import systems.symbol.rdf4j.fedx.FedXEndpoint;

/**
 * Strategy interface for introspecting schema from a data source.
 *
 * <p>Extracts schema metadata (table/entity definitions, columns/properties, types) from a source
 * without fetching actual data. Results are cached by {@code I_SchemaMaterializer}.
 *
 * <p>Implementations exist for each source type:
 * - JDBC: queries DatabaseMetaData for tables, columns, types
 * - API (REST/GraphQL): parses OpenAPI/GraphQL schema from spec
 * - RDF/SPARQL: queries source for class/property definitions
 *
 * <p>Separate from data fetching ({@code I_DataMaterializer}) to allow different refresh
 * frequencies and cache strategies.
 */
public interface I_SourceIntrospector {

  /**
   * Introspect schema from a source.
   *
   * <p>Fetches schema metadata (table/entity definitions, columns, types) without retrieving
   * data. Caller is responsible for caching the result.
   *
   * @param endpoint the virtual graph endpoint with connection details
   * @return immutable schema metadata
   * @throws MaterializationException if introspection fails (connection error, auth failure, etc)
   */
  SourceSchema introspectSchema(FedXEndpoint endpoint) throws MaterializationException;

  /**
   * Get the source type this introspector handles.
   *
   * <p>Examples: "JDBC", "API", "RDF", "SPARQL".
   *
   * @return source type identifier
   */
  String sourceType();

  /**
   * Check if this introspector can handle the given endpoint.
   *
   * <p>Called by router to select appropriate introspector. Typically checks endpoint URL scheme
   * or source type metadata.
   *
   * @param endpoint the virtual graph to check
   * @return true if this introspector can introspect this endpoint
   */
  boolean canHandle(FedXEndpoint endpoint);
}
