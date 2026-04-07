
package systems.symbol.connect.snowflake;

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

public final class SnowflakeConnector extends AbstractConnector {

private final SnowflakeConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(SnowflakeConnector.class);

public SnowflakeConnector(String connectorId, SnowflakeConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:snowflake:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("SNOWFLAKE_API_KEY is required");
}

// Initialize framework components
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("Snowflake sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying Snowflake item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("Snowflake dead-letter: {}", err.itemId));

try {
validateSnowflakeCredentials(config.getApiKey().get());

IRI entity = Values.iri(entityBaseIri().stringValue() + "snowflake-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "SnowflakeResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.literal("Snowflake"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.literal(1), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Snowflake connector sync completed: 1 resource discovered. {}", stats);
} catch (Exception e) {
state.recordFailure("snowflake-sync", e.getMessage());
errorHandler.recordError("snowflake-sync", e);
var stats = state.finish();
log.error("Snowflake connector sync failed: {}", stats, e);
throw e;
}
}

private void validateSnowflakeCredentials(String apiKey) throws IOException, InterruptedException {
try {
HttpClient client = HttpClient.newBuilder().build();
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://api.snowflakepowered.com/databases/list"))
.header("Authorization", "Bearer " + apiKey)
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401 || response.statusCode() == 403) {
throw new IllegalStateException("Snowflake authentication failed: Invalid token");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("Snowflake API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("Snowflake validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
