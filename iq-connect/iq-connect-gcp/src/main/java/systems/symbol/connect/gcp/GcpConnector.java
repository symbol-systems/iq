
package systems.symbol.connect.gcp;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

public final class GcpConnector extends AbstractConnector {

public GcpConnector(String connectorId) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:gcp://"));
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
IRI subject = Values.iri(entityBaseIri().stringValue() + "gcp-sample");
getModel().add(subject, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "GcpResource"), graphIri());
getModel().add(subject, Values.iri(ontologyBaseIri().stringValue() + "hasName"), Values.***REMOVED***("Gcp test"), graphIri());
}
}
