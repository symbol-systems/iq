
package systems.symbol.connect.slack;

import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connector.state.ConnectorState;
import systems.symbol.connect.core.Modeller;

public final class SlackConnector extends AbstractConnector {

private final SlackConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(SlackConnector.class);

public SlackConnector(String connectorId, SlackConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:slack:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("SLACK_API_KEY is required");
}

// Initialize framework components
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("Slack sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying Slack item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("Slack dead-letter: {}", err.itemId));

try {
Slack slack = Slack.getInstance();
MethodsClient methods = slack.methods(config.getApiKey().get());
UsersListResponse usersResult = methods.usersList(req -> req.limit(200));

if (!usersResult.isOk()) {
throw new IllegalStateException("Slack API failed: " + usersResult.getError());
}

int resourceCount = 0;
for (User user : usersResult.getMembers()) {
if (user.getId() == null) continue;
String safeId = user.getId().replaceAll("[^A-Za-z0-9:_-]", "_");
IRI entity = Values.iri(entityBaseIri().stringValue() + safeId);

getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "SlackUser"), graphIri());
if (user.getName() != null) {
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "username"), Values.***REMOVED***(user.getName()), graphIri());
}
if (user.getRealName() != null) {
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "realName"), Values.***REMOVED***(user.getRealName()), graphIri());
}
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "isBot"), Values.***REMOVED***(user.isBot()), graphIri());

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
resourceCount++;
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(resourceCount), graphIri());

state.recordSuccess();
var stats = state.finish();
log.info("Slack connector sync completed: {} resources discovered. {}", resourceCount, stats);
} catch (Exception e) {
state.recordFailure("slack-sync", e.getMessage());
errorHandler.recordError("slack-sync", e);
var stats = state.finish();
log.error("Slack connector sync failed: {}", stats, e);
throw e;
}
}
}
