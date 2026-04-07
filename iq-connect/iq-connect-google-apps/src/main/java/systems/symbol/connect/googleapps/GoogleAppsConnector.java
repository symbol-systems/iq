
package systems.symbol.connect.googleapps;

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

public final class GoogleAppsConnector extends AbstractConnector {

private final GoogleAppsConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(GoogleAppsConnector.class);

public GoogleAppsConnector(String connectorId, GoogleAppsConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:google-apps:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[GoogleApps] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [GoogleApps] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[GoogleApps] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("GOOGLE_APPS_API_KEY is required");
}

validateGoogleAppsCredentials(config.getApiKey().get());

IRI entity = Values.iri(entityBaseIri().stringValue() + "google-apps-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "GoogleAppsResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("GoogleApps"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("GoogleApps connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("googleapps-sync", ex.getMessage());
errorHandler.recordError("googleapps-sync", ex);
var stats = state.finish();
log.error("GoogleApps connector sync failed: {}", stats, ex);
throw ex;
}
}

private void validateGoogleAppsCredentials(String apiKey) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://www.googleapis.com/drive/v3/files?pageSize=1"))
.header("Authorization", "Bearer " + apiKey)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Google Apps authentication failed: Invalid API key or insufficient permissions");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Google Apps API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Google Apps validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
