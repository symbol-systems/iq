
package systems.symbol.connect.databricks;

import java.time.Duration;
import java.util.Optional;

public final class DatabricksConnectorConfig {

private final String host;
private final String apiKey;
private final Duration pollInterval;
private final String graphIri;

public DatabricksConnectorConfig(String host, String apiKey, Duration pollInterval, String graphIri) {
this.host = host;
this.apiKey = apiKey;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.graphIri = graphIri;
}

public Optional<String> getHost() {
return Optional.ofNullable(host).filter(s -> !s.isBlank());
}

public Optional<String> getApiKey() {
return Optional.ofNullable(apiKey).filter(s -> !s.isBlank());
}

public Duration getPollInterval() {
return pollInterval;
}

public Optional<String> getGraphIri() {
return Optional.ofNullable(graphIri).filter(s -> !s.isBlank());
}

public static DatabricksConnectorConfig fromEnv() {
String host = System.getenv("DATABRICKS_HOST");
String apiKey = System.getenv("DATABRICKS_API_KEY");
String graphIri = System.getenv("DATABRICKS_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);
String interval = System.getenv("DATABRICKS_POLL_INTERVAL_SECONDS");
if (interval != null && !interval.isBlank()) {
try { poll = Duration.ofSeconds(Long.parseLong(interval)); } catch (NumberFormatException ignored) { }
}
return new DatabricksConnectorConfig(host, apiKey, poll, graphIri);}
}
