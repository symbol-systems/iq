package systems.symbol.rdf4j.fedx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing a FedX endpoint configuration.
 *
 * <p>
 * Holds metadata about a remote SPARQL endpoint for federation queries:
 * node ID, URL, SPARQL endpoint reference, and optional properties.
 * </p>
 */
public class FedXEndpoint {

  private final String nodeId;
  private final String url;
  private final String sparqlEndpoint;
  private final boolean queryable;
  private final boolean updateable;
  private final Map<String, String> properties;

  /**
   * Create a FedX endpoint.
   *
   * @param nodeId Unique node identifier
   * @param url Base URL of the endpoint
   * @param sparqlEndpoint SPARQL endpoint URI (may be a pseudo-URI like
   *        "urn:iq:sparql:jdbc:..." for virtual graphs)
   */
  public FedXEndpoint(String nodeId, String url, String sparqlEndpoint) {
    this(nodeId, url, sparqlEndpoint, true, false, Collections.emptyMap());
  }

  /**
   * Create a FedX endpoint with full configuration.
   *
   * @param nodeId Unique node identifier
   * @param url Base URL
   * @param sparqlEndpoint SPARQL endpoint URI
   * @param queryable Whether this endpoint supports queries
   * @param updateable Whether this endpoint supports updates
   * @param properties Optional key-value properties (auth, timeouts, etc.)
   */
  public FedXEndpoint(
      String nodeId,
      String url,
      String sparqlEndpoint,
      boolean queryable,
      boolean updateable,
      Map<String, String> properties) {
    this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
    this.url = Objects.requireNonNull(url, "url must not be null");
    this.sparqlEndpoint = Objects.requireNonNull(sparqlEndpoint, "sparqlEndpoint must not be null");
    this.queryable = queryable;
    this.updateable = updateable;
    this.properties = new HashMap<>(properties);
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getUrl() {
    return url;
  }

  public String getSparqlEndpoint() {
    return sparqlEndpoint;
  }

  public boolean isQueryable() {
    return queryable;
  }

  public boolean isUpdateable() {
    return updateable;
  }

  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public String toString() {
    return "FedXEndpoint{"
        + "nodeId='"
        + nodeId
        + '\''
        + ", url='"
        + url
        + '\''
        + ", sparqlEndpoint='"
        + sparqlEndpoint
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FedXEndpoint)) return false;
    FedXEndpoint that = (FedXEndpoint) o;
    return Objects.equals(nodeId, that.nodeId)
        && Objects.equals(sparqlEndpoint, that.sparqlEndpoint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, sparqlEndpoint);
  }
}
