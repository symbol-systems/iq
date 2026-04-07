
package systems.symbol.connect.stripe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;

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

public final class StripeConnector extends AbstractConnector {

private final StripeConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(StripeConnector.class);

public StripeConnector(String connectorId, StripeConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:stripe:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Stripe] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Stripe] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Stripe] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("STRIPE_API_KEY is required");
}

validateStripeCredentials(config.getApiKey().get());

IRI entity = Values.iri(entityBaseIri().stringValue() + "stripe-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "StripeResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("Stripe"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Stripe connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("stripe-sync", ex.getMessage());
errorHandler.recordError("stripe-sync", ex);
var stats = state.finish();
log.error("Stripe connector sync failed: {}", stats, ex);
throw ex;
}
}

private void validateStripeCredentials(String apiKey) throws IOException, InterruptedException {
try {
String basicAuth = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://api.stripe.com/v1/charges?limit=1"))
.header("Authorization", basicAuth)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Stripe authentication failed: Invalid API key");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Stripe API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Stripe validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
