
package systems.symbol.connect.datadog;

import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.Modeller;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connector.state.ConnectorState;

public final class DatadogConnector extends AbstractConnector {

private final DatadogConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(DatadogConnector.class);

public DatadogConnector(String connectorId, DatadogConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:datadog:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Datadog] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Datadog] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Datadog] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty() || config.getAppKey().isEmpty()) {
throw new IllegalStateException("DATADOG_API_KEY and DATADOG_APP_KEY are required");
}

OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
.url("https://api.datadoghq.com/api/v1/monitor")
.addHeader("DD-API-KEY", config.getApiKey().get())
.addHeader("DD-APPLICATION-KEY", config.getAppKey().get())
.build();

try (Response response = client.newCall(request).execute()) {
if (!response.isSuccessful()) {
throw new IllegalStateException("Datadog API call failed: " + response.code() + " " + response.message());
}

ObjectMapper mapper = new ObjectMapper();
JsonNode root = mapper.readTree(response.body().string());
if (!root.isArray()) {
throw new IllegalStateException("Unexpected Datadog monitor response format");
}

int count = 0;
for (JsonNode monitor : root) {
if (!monitor.hasNonNull("id")) continue;

String id = monitor.get("id").asText();
String safeId = id.replaceAll("[^A-Za-z0-9:_-]", "_");
IRI monitorNode = Values.iri(entityBaseIri().stringValue() + safeId);

getModel().add(monitorNode, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "DatadogMonitor"), graphIri());
if (monitor.hasNonNull("name")) {
getModel().add(monitorNode, Values.iri(ontologyBaseIri().stringValue() + "name"), Values.***REMOVED***(monitor.get("name").asText()), graphIri());
}
if (monitor.hasNonNull("type")) {
getModel().add(monitorNode, Values.iri(ontologyBaseIri().stringValue() + "type"), Values.***REMOVED***(monitor.get("type").asText()), graphIri());
}
if (monitor.hasNonNull("query")) {
getModel().add(monitorNode, Values.iri(ontologyBaseIri().stringValue() + "query"), Values.***REMOVED***(monitor.get("query").asText()), graphIri());
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), monitorNode, graphIri());
count++;
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(count), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Datadog connector sync completed: {}", stats);
}
} catch (Exception ex) {
state.recordFailure("datadog-sync", ex.getMessage());
errorHandler.recordError("datadog-sync", ex);
var stats = state.finish();
log.error("Datadog connector sync failed: {}", stats, ex);
throw ex;
}
}
}
