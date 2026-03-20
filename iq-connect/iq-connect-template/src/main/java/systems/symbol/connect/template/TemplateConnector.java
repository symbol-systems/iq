package systems.symbol.connect.template;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

/**
 * Example connector implementation that demonstrates the minimal IQ connector contract.
 * <p>
 * This is a template and not intended for production use.
 */
public final class TemplateConnector extends AbstractConnector {

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
protected void doRefresh() {
// no-op in template; implement sync logic in connectors
}
}
