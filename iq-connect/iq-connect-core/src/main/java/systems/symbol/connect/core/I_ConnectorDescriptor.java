package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;

/**
 * A descriptor for a connector instance (metadata that can be registered in a discovery graph).
 */
public interface I_ConnectorDescriptor {

    /** Returns the stable IRI for the connector instance. */
    IRI getSelf();

    /**
     * Updates a model to describes this connector (for registration/discovery).
     *
     * <p>The model should contain a minimal set of triples needed to discover the connector
     * and its capabilities; it is safe for this view to be a subset of the full connector state.</p>
     */
    void describe(Model model);
}
