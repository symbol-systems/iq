
package systems.symbol.connect.docker;

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

public final class DockerConnector extends AbstractConnector {

private final DockerConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(DockerConnector.class);

public DockerConnector(String connectorId, DockerConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:docker:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Docker] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Docker] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Docker] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("DOCKER_API_KEY is required");
}

// Validate token by making an API call to Docker
validateDockerCredentials(config.getApiKey().get());

// Minimal discovered data path to keep key functionality of a connector
IRI entity = Values.iri(entityBaseIri().stringValue() + "docker-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "DockerResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.literal("Docker"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.literal(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Docker connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("docker-sync", ex.getMessage());
errorHandler.recordError("docker-sync", ex);
var stats = state.finish();
log.error("Docker connector sync failed: {}", stats, ex);
throw ex;
}
}

/**
 * Validates the Docker API credentials by making a simple request.
 * Throws an exception if the credentials are invalid or authentication fails.
 */
private void validateDockerCredentials(String apiKey) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://hub.docker.com/v2/users/login"))
.header("Content-Type", "application/json")
.POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"test\",\"password\":\"" + apiKey + "\"}"))
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401) {
throw new IllegalStateException("Docker authentication failed: Invalid API key");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Docker API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Docker validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
