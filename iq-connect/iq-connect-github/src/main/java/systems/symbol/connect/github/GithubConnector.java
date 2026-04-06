
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
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
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

// Wire GitHub scanner classes into discovery pipeline
try {
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
  Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), 
  Values.***REMOVED***(resourceCount), graphIri());

log.info("GitHub connector sync completed: {} resources discovered", resourceCount);
} catch (IOException e) {
log.error("GitHub API error during refresh: {}", e.getMessage(), e);
throw new Exception("GitHub discovery failed: " + e.getMessage(), e);
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
