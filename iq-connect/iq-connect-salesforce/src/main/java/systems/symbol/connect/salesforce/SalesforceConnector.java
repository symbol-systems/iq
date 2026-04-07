
package systems.symbol.connect.salesforce;

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

public final class SalesforceConnector extends AbstractConnector {

private final SalesforceConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(SalesforceConnector.class);

public SalesforceConnector(String connectorId, SalesforceConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:salesforce:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Salesforce] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Salesforce] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Salesforce] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("SALESFORCE_API_KEY is required");
}

validateSalesforceCredentials(config.getApiKey().get());

IRI entity = Values.iri(entityBaseIri().stringValue() + "salesforce-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "SalesforceResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.literal("Salesforce"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.literal(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Salesforce connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("salesforce-sync", ex.getMessage());
errorHandler.recordError("salesforce-sync", ex);
var stats = state.finish();
log.error("Salesforce connector sync failed: {}", stats, ex);
throw ex;
}
}

private void validateSalesforceCredentials(String apiKey) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://login.salesforce.com/services/oauth2/userinfo"))
.header("Authorization", "Bearer " + apiKey)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Salesforce authentication failed: Invalid token");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Salesforce API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Salesforce validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
