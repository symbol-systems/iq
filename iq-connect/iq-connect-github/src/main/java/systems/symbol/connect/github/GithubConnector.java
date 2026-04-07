
package systems.symbol.connect.github;

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
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connector.state.ConnectorState;
import systems.symbol.connect.core.Modeller;

public final class GithubConnector extends AbstractConnector {

private final GithubConnectorConfig config;
private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
private static final Logger log = LoggerFactory.getLogger(GithubConnector.class);

public GithubConnector(String connectorId, GithubConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:github:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("GITHUB_API_KEY is required");
}

String token = config.getApiKey().get();

// Validate token by making an API call to GitHub
validateGithubToken(token);

// Initialize framework components
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("GitHub sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying GitHub item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("GitHub dead-letter: {}", err.itemId));

try {
// Wire GitHub scanner classes into discovery pipeline
GitHub github = GitHub.connectUsingOAuth(token);
GithubModeller modeller = new GithubModeller(
getModel(),
graphIri(),
ontologyBaseIri(),
entityBaseIri()
);
GithubScanContext context = new GithubScanContext(getConnectorId(), modeller);

// Scan authenticated user and their resources
new GithubMyselfScanner(github.getMyself()).scan(context);

// Update connector metadata
long resourceCount = getModel().stream()
.filter(stmt -> stmt.getPredicate().stringValue().equals(ConnectorModels.HAS_RESOURCE))
.count();

getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), 
  Values.literal(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), 
  Values.literal(resourceCount), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("GitHub connector sync completed: {} resources discovered.{}", resourceCount, stats);
} catch (IOException e) {
state.recordFailure("github-api", e.getMessage());
errorHandler.recordError("github-api", e);
var stats = state.finish();
log.error("GitHub connector sync failed: {}", stats, e);
throw new Exception("GitHub discovery failed: " + e.getMessage(), e);
} catch (Exception e) {
state.recordFailure("github-sync", e.getMessage());
errorHandler.recordError("github-sync", e);
var stats = state.finish();
log.error("GitHub connector sync failed: {}", stats, e);
throw e;
}
}

/**
 * Validates the GitHub API token by making a simple GET request to the user endpoint.
 * Throws an exception if the token is invalid or authentication fails.
 */
private void validateGithubToken(String token) throws IOException, InterruptedException {
try {
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://api.github.com/user"))
.header("Authorization", "token " + token)
.header("Accept", "application/vnd.github.v3+json")
.GET()
.build();

HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 401) {
throw new IllegalStateException("GitHub authentication failed: Invalid token or expired credentials");
}

if (response.statusCode() >= 400) {
throw new IllegalStateException("GitHub API error: " + response.statusCode());
}
} catch (IllegalStateException e) {
throw e;
} catch (Exception e) {
throw new IllegalStateException("GitHub validation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
}
}
}
