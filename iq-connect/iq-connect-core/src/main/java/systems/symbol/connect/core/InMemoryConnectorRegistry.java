package systems.symbol.connect.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;

/**
 * Simple in-memory connector registry.
 */
public final class InMemoryConnectorRegistry implements I_ConnectorRegistry {

    private final Map<String, I_Connector> connectors = new LinkedHashMap<>();

    @Override
    public synchronized void register(I_Connector connector) {
        connectors.put(connector.getConnectorId().toString(), connector);
    }

    @Override
    public synchronized void unregister(IRI connectorId) {
        connectors.remove(connectorId.toString());
    }

    @Override
    public synchronized Optional<I_Connector> get(IRI connectorId) {
        return Optional.ofNullable(connectors.get(connectorId.toString()));
    }

    @Override
    public synchronized Iterable<I_Connector> list() {
        return Collections.unmodifiableCollection(connectors.values());
    }
}
