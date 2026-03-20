package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.IRI;

/**
 * A descriptor for a connector instance. Provides the stable IRI used for runtime lookup.
 */
public interface I_ConnectorDescriptor {

    /** Returns the stable IRI for the connector instance. */
    IRI getSelf();

}
