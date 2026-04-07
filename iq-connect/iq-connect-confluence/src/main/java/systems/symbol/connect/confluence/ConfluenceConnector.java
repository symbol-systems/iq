package systems.symbol.connect.confluence;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connector.state.ConnectorState;

public final class ConfluenceConnector extends AbstractConnector {

private final ConfluenceConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(ConfluenceConnector.class);

public ConfluenceConnector(String connectorId, ConfluenceConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:confluence:"));
this.config = config;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

public java.time.Duration getPollInterval() {
return config.getPollInterval();
}

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Confluence] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Confluence] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Confluence] dead-letter: {}", err.itemId));

try {
if (config.getBaseUrl().isEmpty()) {
throw new IllegalStateException("CONFLUENCE_BASE_URL is required");
}

if (config.getApiToken().isEmpty() && (config.getUsername().isEmpty() || config.getPassword().isEmpty())) {
throw new IllegalStateException("Confluence credentials are required (API token or username/password)");
}

// Validate credentials with actual API call
validateConfluenceCredentials();

IRI space = Values.iri(entityBaseIri().stringValue() + "space-1");
getModel().add(space, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "ConfluenceSpace"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "name"), Values.literal("Default Space"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "key"), Values.literal("DS"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.literal(Instant.now().toString()), graphIri());

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), space, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.literal(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Confluence connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("confluence-sync", ex.getMessage());
errorHandler.recordError("confluence-sync", ex);
var stats = state.finish();
log.error("Confluence connector sync failed: {}", stats, ex);
throw ex;
}
}

private void validateConfluenceCredentials() throws IOException, InterruptedException {
try {
String baseUrl = config.getBaseUrl().get();
if (!baseUrl.endsWith("/")) {
baseUrl += "/";
}

HttpClient client = HttpClient.newBuilder().build();
HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
.uri(URI.create(baseUrl + "rest/api/2/user/current"));

if (config.getApiToken().isPresent()) {
String token = config.getApiToken().get();
String basicAuth = "Basic " + Base64.getEncoder().encodeToString(("apiToken:" + token).getBytes());
requestBuilder.header("Authorization", basicAuth);
} else if (config.getUsername().isPresent() && config.getPassword().isPresent()) {
String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
(config.getUsername().get() + ":" + config.getPassword().get()).getBytes());
requestBuilder.header("Authorization", basicAuth);
} else {
throw new IllegalStateException("No valid Confluence credentials provided");
}

HttpRequest request = requestBuilder.GET().build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Confluence authentication failed: Invalid credentials");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Confluence API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Confluence validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
