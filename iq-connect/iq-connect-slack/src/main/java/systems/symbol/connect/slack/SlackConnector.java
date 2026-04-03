
package systems.symbol.connect.slack;

import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.Modeller;

public final class SlackConnector extends AbstractConnector {

private final SlackConnectorConfig config;

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
}
}
