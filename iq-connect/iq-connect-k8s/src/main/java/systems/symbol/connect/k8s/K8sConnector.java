
package systems.symbol.connect.k8s;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

public final class K8sConnector extends AbstractConnector {

public K8sConnector(String connectorId) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:k8s://"));
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
IRI subject = Values.iri(entityBaseIri().stringValue() + "k8s-sample");
getModel().add(subject, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "K8sResource"), graphIri());
getModel().add(subject, Values.iri(ontologyBaseIri().stringValue() + "hasName"), Values.***REMOVED***("K8s test"), graphIri());
}
}
