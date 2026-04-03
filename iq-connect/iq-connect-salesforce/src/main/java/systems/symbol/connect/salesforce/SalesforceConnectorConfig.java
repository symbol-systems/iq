
package systems.symbol.connect.salesforce;

import java.time.Duration;
import java.util.Optional;

public final class SalesforceConnectorConfig {

private final String apiKey;
private final Duration pollInterval;
private final String graphIri;

public SalesforceConnectorConfig(String apiKey, Duration pollInterval, String graphIri) {
this.apiKey = apiKey;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.graphIri = graphIri;
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

public static SalesforceConnectorConfig fromEnv() {
String apiKey = System.getenv("SALESFORCE_API_KEY");
String graphIri = System.getenv("SALESFORCE_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);
String interval = System.getenv("SALESFORCE_POLL_INTERVAL_SECONDS");
if (interval != null && !interval.isBlank()) {
try { poll = Duration.ofSeconds(Long.parseLong(interval)); } catch (NumberFormatException ignored) { }
}
return new SalesforceConnectorConfig(apiKey, poll, graphIri);
}
}
