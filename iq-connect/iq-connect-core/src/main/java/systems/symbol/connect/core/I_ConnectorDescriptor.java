package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;

/**
 * A descriptor for a connector instance (metadata that can be registered in a discovery graph).
 */
public interface I_ConnectorDescriptor {

    /** Returns the stable IRI for the connector instance. */
    IRI getConnectorId();

    /** Returns a human-friendly name for the connector. */
    String getName();

    /** Returns a human-friendly description of the connector. */
    String getDescription();

    /**
     * Returns a model that describes this connector (for registration/discovery).
     *
     * <p>The returned model should contain a minimal set of triples needed to discover the connector
     * and its capabilities; it is safe for this view to be a subset of the full connector state.</p>
     */
    Model getDescriptorModel();
}
