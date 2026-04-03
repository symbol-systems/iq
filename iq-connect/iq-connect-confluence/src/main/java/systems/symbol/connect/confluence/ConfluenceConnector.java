package systems.symbol.connect.confluence;

import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

public final class ConfluenceConnector extends AbstractConnector {

private final ConfluenceConnectorConfig config;

public ConfluenceConnector(String connectorId, ConfluenceConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:confluence:"));
this.config = config;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

public java.time.Duration getPollInterval() {
return config.getPollInterval();
}

@Override
protected void doRefresh() throws Exception {
if (config.getBaseUrl().isEmpty()) {
throw new IllegalStateException("CONFLUENCE_BASE_URL is required");
}

if (config.getApiToken().isEmpty() && (config.getUsername().isEmpty() || config.getPassword().isEmpty())) {
throw new IllegalStateException("Confluence credentials are required (API token or username/password)");
}

IRI space = Values.iri(entityBaseIri().stringValue() + "space-1");
getModel().add(space, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "ConfluenceSpace"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "name"), Values.***REMOVED***("Default Space"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "key"), Values.***REMOVED***("DS"), graphIri());
getModel().add(space, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), space, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());
}
}
