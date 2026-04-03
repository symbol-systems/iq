package systems.symbol.connect.azure;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.time.Duration;

public final class AzureConnectorConfig {

private final String subscriptionId;
private final String tenantId;
private final Duration pollInterval;
private final Set<String> scanServices;
private final String graphIri;

public AzureConnectorConfig(String subscriptionId,
String tenantId,
Duration pollInterval,
Set<String> scanServices,
String graphIri) {
this.subscriptionId = subscriptionId;
this.tenantId = tenantId;
this.pollInterval = pollInterval == null ? Duration.ofMinutes(5) : pollInterval;
this.scanServices = scanServices == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(scanServices));
this.graphIri = graphIri;
}

public Optional<String> getSubscriptionId() {
return Optional.ofNullable(subscriptionId).filter(s -> !s.isBlank());
}

public Optional<String> getTenantId() {
return Optional.ofNullable(tenantId).filter(s -> !s.isBlank());
}

public Duration getPollInterval() {
return pollInterval;
}

public Set<String> getScanServices() {
return scanServices;
}

public Optional<String> getGraphIri() {
return Optional.ofNullable(graphIri).filter(s -> !s.isBlank());
}

public static AzureConnectorConfig fromEnv() {
String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
String tenantId = System.getenv("AZURE_TENANT_ID");
String interval = System.getenv("AZURE_POLL_INTERVAL_SECONDS");
Duration poll = Duration.ofMinutes(5);
if (interval != null && !interval.isBlank()) {
try {
poll = Duration.ofSeconds(Long.parseLong(interval));
} catch (NumberFormatException e) {
// fallback to default
}
}
String graphIri = System.getenv("AZURE_GRAPH_IRI");

String serviceList = System.getenv("AZURE_SCAN_SERVICES");
Set<String> scanServices = Collections.emptySet();
if (serviceList != null && !serviceList.isBlank()) {
scanServices = new HashSet<>(Arrays.asList(serviceList.split(",")));
}

return new AzureConnectorConfig(subscriptionId, tenantId, poll, scanServices, graphIri);
}
}
