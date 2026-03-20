package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;

/**
 * A descriptor for a connector instance (metadata that can be registered in a discovery graph).
 */
public interface I_ConnectorDescriptor {

    /** Returns the stable IRI for the connector instance. */
    IRI getSelf();

    /** Human-readable connector name for discovery. */
    String getName();

    /** Human-readable connector description for discovery. */
    String getDescription();

    /**
     * Returns the descriptor model describing this connector (for registration/discovery).
     */
    Model getDescriptorModel();

    /**
     * Updates a model to describe this connector (for registration/discovery).
     *
     * <p>The model should contain a minimal set of triples needed to discover the connector
     * and its capabilities; it is safe for this view to be a subset of the full connector state.</p>
     */
    default void describe(Model model) {
        model.addAll(getDescriptorModel());
    }
}
