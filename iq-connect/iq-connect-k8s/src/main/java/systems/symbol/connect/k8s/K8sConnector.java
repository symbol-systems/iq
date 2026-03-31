
package systems.symbol.connect.k8s;

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
            Values.iri("urn:k8s:"));
    }

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.READ_ONLY;
    }

    @Override
    protected void doRefresh() {
        // TODO: implement K8s sync behavior
    }
}
