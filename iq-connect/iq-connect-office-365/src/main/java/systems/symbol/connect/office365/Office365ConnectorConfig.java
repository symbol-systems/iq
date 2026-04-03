
package systems.symbol.connect.office365;

import java.time.Duration;
import java.util.Optional;

public final class Office365ConnectorConfig {

private final String apiKey;
private final Duration pollInterval;
private final String graphIri;

public Office365ConnectorConfig(String apiKey, Duration pollInterval, String graphIri) {
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

public static Office365ConnectorConfig fromEnv() {
String apiKey = System.getenv("OFFICE_365_API_KEY");
String graphIri = System.getenv("OFFICE_365_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);
String interval = System.getenv("OFFICE_365_POLL_INTERVAL_SECONDS");
if (interval != null && !interval.isBlank()) {
try { poll = Duration.ofSeconds(Long.parseLong(interval)); } catch (NumberFormatException ignored) { }
}
return new Office365ConnectorConfig(apiKey, poll, graphIri);
}
}
