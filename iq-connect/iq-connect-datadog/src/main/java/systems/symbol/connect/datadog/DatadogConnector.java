
package systems.symbol.connect.datadog;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

public final class DatadogConnector extends AbstractConnector {

public DatadogConnector(String connectorId) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:datadog://"));
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
IRI subject = Values.iri(entityBaseIri().stringValue() + "datadog-sample");
getModel().add(subject, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "DatadogResource"), graphIri());
getModel().add(subject, Values.iri(ontologyBaseIri().stringValue() + "hasName"), Values.***REMOVED***("Datadog test"), graphIri());
}
}
