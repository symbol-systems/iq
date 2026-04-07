
package systems.symbol.connect.databricks;

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

public final class DatabricksConnector extends AbstractConnector {

private final DatabricksConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(DatabricksConnector.class);

public DatabricksConnector(String connectorId, DatabricksConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:databricks:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Databricks] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Databricks] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Databricks] dead-letter: {}", err.itemId));

try {
if (config.getApiKey().isEmpty() || config.getHost().isEmpty()) {
throw new IllegalStateException("DATABRICKS_HOST and DATABRICKS_API_KEY are required");
}

String url = config.getHost().get();
if (!url.endsWith("/")) {
url += "/";
}
url += "api/2.0/clusters/list";

OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
.url(url)
.addHeader("Authorization", "Bearer " + config.getApiKey().get())
.build();

try (Response response = client.newCall(request).execute()) {
if (!response.isSuccessful()) {
throw new IllegalStateException("Databricks API request failed: " + response.code() + " " + response.message());
}

ObjectMapper mapper = new ObjectMapper();
JsonNode root = mapper.readTree(response.body().string());
JsonNode clusters = root.path("clusters");
if (!clusters.isArray()) {
throw new IllegalStateException("Databricks clusters response missing array");
}

int counted = 0;
for (JsonNode cluster : clusters) {
String clusterId = cluster.path("cluster_id").asText("unknown");
String safeId = clusterId.replaceAll("[^A-Za-z0-9:_-]", "_");
IRI resource = Values.iri(entityBaseIri().stringValue() + safeId);

getModel().add(resource, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "DatabricksCluster"), graphIri());
if (cluster.hasNonNull("cluster_name")) {
getModel().add(resource, Values.iri(ontologyBaseIri().stringValue() + "clusterName"), Values.***REMOVED***(cluster.get("cluster_name").asText()), graphIri());
}
if (cluster.hasNonNull("state")) {
getModel().add(resource, Values.iri(ontologyBaseIri().stringValue() + "state"), Values.***REMOVED***(cluster.get("state").asText()), graphIri());
}
if (cluster.hasNonNull("spark_version")) {
getModel().add(resource, Values.iri(ontologyBaseIri().stringValue() + "sparkVersion"), Values.***REMOVED***(cluster.get("spark_version").asText()), graphIri());
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), resource, graphIri());
counted++;
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(counted), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Databricks connector sync completed: {}", stats);
}
} catch (Exception ex) {
state.recordFailure("databricks-sync", ex.getMessage());
errorHandler.recordError("databricks-sync", ex);
var stats = state.finish();
log.error("Databricks connector sync failed: {}", stats, ex);
throw ex;
}
}
}
