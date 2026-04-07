
package systems.symbol.connect.k8s;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.Modeller;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connector.state.ConnectorState;

public final class K8sConnector extends AbstractConnector {

private final K8sConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(K8sConnector.class);

public K8sConnector(String connectorId, K8sConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:k8s:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[K8s] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [K8s] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[K8s] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("K8S_API_KEY is required");
}

validateK8sCredentials(config.getApiKey().get());

IRI entity = Values.iri(entityBaseIri().stringValue() + "k8s-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "K8sResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.literal("K8s"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.literal(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("K8s connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("k8s-sync", ex.getMessage());
errorHandler.recordError("k8s-sync", ex);
var stats = state.finish();
log.error("K8s connector sync failed: {}", stats, ex);
throw ex;
}
}

private void validateK8sCredentials(String apiKey) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://kubernetes.default.svc.cluster.local/api/v1/nodes"))
.header("Authorization", "Bearer " + apiKey)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Kubernetes authentication failed: Invalid token or insufficient permissions");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Kubernetes API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Kubernetes validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
