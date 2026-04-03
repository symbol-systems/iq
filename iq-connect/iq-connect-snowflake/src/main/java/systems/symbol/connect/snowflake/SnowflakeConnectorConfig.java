
package systems.symbol.connect.snowflake;

import java.time.Duration;
import java.util.Optional;

public final class SnowflakeConnectorConfig {

private final String apiKey;
private final Duration pollInterval;
private final String graphIri;

public SnowflakeConnectorConfig(String apiKey, Duration pollInterval, String graphIri) {
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

public static SnowflakeConnectorConfig fromEnv() {
String apiKey = System.getenv("SNOWFLAKE_API_KEY");
String graphIri = System.getenv("SNOWFLAKE_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);
String interval = System.getenv("SNOWFLAKE_POLL_INTERVAL_SECONDS");
if (interval != null && !interval.isBlank()) {
try { poll = Duration.ofSeconds(Long.parseLong(interval)); } catch (NumberFormatException ignored) { }
}
return new SnowflakeConnectorConfig(apiKey, poll, graphIri);
}
}
