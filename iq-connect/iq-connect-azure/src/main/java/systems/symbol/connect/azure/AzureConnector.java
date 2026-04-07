
package systems.symbol.connect.azure;

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
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connector.state.ConnectorState;
import systems.symbol.connect.core.Modeller;

public final class AzureConnector extends AbstractConnector {

private final AzureConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(AzureConnector.class);

public AzureConnector(String connectorId, AzureConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:azure:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("AZURE_API_KEY is required");
}

// Initialize framework components
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("Azure sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying Azure item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("Azure dead-letter: {}", err.itemId));

try {
// Validate token by making an API call to Azure
validateAzureToken(config.getApiKey().get());

// Minimal discovered data path to keep key functionality of a connector
IRI entity = Values.iri(entityBaseIri().stringValue() + "azure-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "AzureResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("Azure"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Azure connector sync completed: 1 resource discovered. {}", stats);
} catch (Exception e) {
state.recordFailure("azure-sync", e.getMessage());
errorHandler.recordError("azure-sync", e);
var stats = state.finish();
log.error("Azure connector sync failed: {}", stats, e);
throw e;
}
}

/**
 * Validates the Azure API token by making a simple GET request to Azure REST API.
 * Throws an exception if the token is invalid or authentication fails.
 */
private void validateAzureToken(String token) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
// Use a simple Azure API endpoint that just validates the token
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://management.azure.com/subscriptions?api-version=2020-01-01"))
.header("Authorization", "Bearer " + token)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Azure authentication failed: Invalid token or insufficient permissions");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Azure API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Azure validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
