
package systems.symbol.connect.datadog;

import java.time.Duration;
import java.util.Optional;

public final class DatadogConnectorConfig {

private final String apiKey;
private final String appKey;
private final Duration pollInterval;
private final String graphIri;

public DatadogConnectorConfig(String apiKey, String appKey, Duration pollInterval, String graphIri) {
this.apiKey = apiKey;
this.appKey = appKey;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.graphIri = graphIri;
}

public DatadogConnectorConfig(String apiKey, Duration pollInterval, String graphIri) {
this(apiKey, null, pollInterval, graphIri);
}

public Optional<String> getApiKey() {
return Optional.ofNullable(apiKey).filter(s -> !s.isBlank());
}

public Optional<String> getAppKey() {
return Optional.ofNullable(appKey).filter(s -> !s.isBlank());
}

public Duration getPollInterval() {
return pollInterval;
}

public Optional<String> getGraphIri() {
return Optional.ofNullable(graphIri).filter(s -> !s.isBlank());
}

public static DatadogConnectorConfig fromEnv() {
String apiKey = System.getenv("DATADOG_API_KEY");
String appKey = System.getenv("DATADOG_APP_KEY");
String graphIri = System.getenv("DATADOG_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);
String interval = System.getenv("DATADOG_POLL_INTERVAL_SECONDS");
if (interval != null && !interval.isBlank()) {
try { poll = Duration.ofSeconds(Long.parseLong(interval)); } catch (NumberFormatException ignored) { }
}
return new DatadogConnectorConfig(apiKey, appKey, poll, graphIri);
}
}
