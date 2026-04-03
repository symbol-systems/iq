package systems.symbol.connect.confluence;

import java.time.Duration;
import java.util.Optional;

public final class ConfluenceConnectorConfig {

private final String baseUrl;
private final String apiToken;
private final String username;
private final String password;
private final Duration pollInterval;
private final String graphIri;
private final String scanAreas;
private final boolean includeArchived;

public ConfluenceConnectorConfig(String baseUrl,
 String apiToken,
 String username,
 String password,
 Duration pollInterval,
 String graphIri,
 String scanAreas,
 Boolean includeArchived) {
this.baseUrl = baseUrl;
this.apiToken = apiToken;
this.username = username;
this.password = password;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.graphIri = graphIri;
this.scanAreas = scanAreas;
this.includeArchived = includeArchived == null ? false : includeArchived;
}

public Optional<String> getBaseUrl() {
return Optional.ofNullable(baseUrl).filter(s -> !s.isBlank());
}

public Optional<String> getApiToken() {
return Optional.ofNullable(apiToken).filter(s -> !s.isBlank());
}

public Optional<String> getUsername() {
return Optional.ofNullable(username).filter(s -> !s.isBlank());
}

public Optional<String> getPassword() {
return Optional.ofNullable(password).filter(s -> !s.isBlank());
}

public Duration getPollInterval() {
return pollInterval;
}

public Optional<String> getGraphIri() {
return Optional.ofNullable(graphIri).filter(s -> !s.isBlank());
}

public Optional<String> getScanAreas() {
return Optional.ofNullable(scanAreas).filter(s -> !s.isBlank());
}

public boolean isIncludeArchived() {
return includeArchived;
}

public static ConfluenceConnectorConfig fromEnv() {
String baseUrl = System.getenv("CONFLUENCE_BASE_URL");
String apiToken = System.getenv("CONFLUENCE_API_TOKEN");
String username = System.getenv("CONFLUENCE_USERNAME");
String password = System.getenv("CONFLUENCE_PASSWORD");
String graphIri = System.getenv("CONFLUENCE_GRAPH_IRI");
Duration poll = Duration.ofMinutes(5);

String pollInterval = System.getenv("CONFLUENCE_POLL_INTERVAL_SECONDS");
if (pollInterval != null && !pollInterval.isBlank()) {
try {
poll = Duration.ofSeconds(Long.parseLong(pollInterval));
} catch (NumberFormatException ignored) {
}
}

String scanAreas = System.getenv("CONFLUENCE_SCAN_AREAS");
Boolean includeArchived = Boolean.parseBoolean(System.getenv("CONFLUENCE_INCLUDE_ARCHIVED"));

return new ConfluenceConnectorConfig(baseUrl, apiToken, username, password, poll, graphIri, scanAreas, includeArchived);
}
}
