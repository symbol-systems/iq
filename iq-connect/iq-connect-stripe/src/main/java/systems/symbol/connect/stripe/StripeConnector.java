
package systems.symbol.connect.stripe;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.Modeller;

public final class StripeConnector extends AbstractConnector {

    public StripeConnector(String connectorId) {
        super(connectorId,
            new LinkedHashModel(),
            Values.iri(connectorId + "/graph/current"),
            Values.iri(Modeller.getConnectOntology()),
            Values.iri("urn:stripe:"));
    }

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.READ_ONLY;
    }

    @Override
    protected void doRefresh() {
        // TODO: implement Stripe sync behavior
    }
}
