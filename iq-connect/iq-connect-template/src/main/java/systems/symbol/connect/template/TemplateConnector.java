package systems.symbol.connect.template;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;
import systems.symbol.connector.error.ConnectorErrorHandler;
import systems.symbol.connector.state.ConnectorState;

/**
 * Example connector implementation that demonstrates the minimal IQ connector contract.
 * <p>
 * This is a template and not intended for production use.
 */
public final class TemplateConnector extends AbstractConnector {

private static final Logger log = LoggerFactory.getLogger(TemplateConnector.class);

public TemplateConnector(String connectorId) {
super(connectorId,
new LinkedHashModel(),
Values.iri(connectorId + "/graph/current"),
Values.iri(Modeller.getConnectOntology()),
Values.iri("urn:template:"));
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("[Template] sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying [Template] item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("[Template] dead-letter: {}", err.itemId));

try {
TemplateModeller modeller = new TemplateModeller(getModel(), graphIri(), ontologyBaseIri(), entityBaseIri());

modeller.createTemplateItem(
"default",
"Template sample item",
"Automatically generated sample entity to demonstrate real sync behavior.");

state.recordSuccess();
var stats = state.finish();
log.info("Template connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("template-sync", ex.getMessage());
errorHandler.recordError("template-sync", ex);
var stats = state.finish();
log.error("Template connector sync failed: {}", stats, ex);
throw ex;
}
}
}
