package systems.symbol.rdf4j.fedx;

import java.util.Objects;

/**
 * Immutable value type representing a SPARQL federation endpoint.
 * Each endpoint is a remote RDF store accessible via HTTP SPARQL interface.
 */
public final class FedXEndpoint {
private final String nodeId;
private final String url;
private final String sparqlEndpoint;
private final boolean queryable;
private final boolean updateable;

public FedXEndpoint(String nodeId, String url, String sparqlEndpoint, 
   boolean queryable, boolean updateable) {
this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
this.url = Objects.requireNonNull(url, "url");
this.sparqlEndpoint = Objects.requireNonNull(sparqlEndpoint, "sparqlEndpoint");
this.queryable = queryable;
this.updateable = updateable;
}

/**
 * Convenience constructor: derive SPARQL endpoint from base URL.
 */
public FedXEndpoint(String nodeId, String url) {
this(nodeId, url, url.endsWith("/") ? url + "sparql" : url + "/sparql", true, false);
}

public String nodeId() {
return nodeId;
}

public String url() {
return url;
}

public String sparqlEndpoint() {
return sparqlEndpoint;
}

public boolean isQueryable() {
return queryable;
}

public boolean isUpdateable() {
return updateable;
}

@Override
public boolean equals(Object o) {
if (this == o) return true;
if (o == null || getClass() != o.getClass()) return false;
FedXEndpoint that = (FedXEndpoint) o;
return nodeId.equals(that.nodeId);
}

@Override
public int hashCode() {
return Objects.hash(nodeId);
}

@Override
public String toString() {
return "FedXEndpoint{" + "nodeId='" + nodeId + '\'' + ", url='" + url + '\'' + '}';
}
}
