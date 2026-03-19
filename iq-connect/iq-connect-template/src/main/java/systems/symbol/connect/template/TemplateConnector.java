package systems.symbol.connect.template;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.I_Checkpoint;
import systems.symbol.connect.core.I_Connector;

/**
 * Example connector implementation that demonstrates the minimal IQ connector contract.
 * <p>
 * This is a template and not intended for production use.
 */
public final class TemplateConnector implements I_Connector {

private final IRI connectorId;
private final Model state;
private volatile ConnectorStatus status = ConnectorStatus.IDLE;
private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

public TemplateConnector(String connectorId) {
this.connectorId = Values.iri(connectorId);
this.state = new LinkedHashModel();
}

@Override
public IRI getConnectorId() {
return connectorId;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
public ConnectorStatus getStatus() {
return status;
}

@Override
public Model getStateModel() {
return state;
}

@Override
public Optional<I_Checkpoint> getCheckpoint() {
return checkpoint;
}

@Override
public void start() {
status = ConnectorStatus.SYNCING;
}

@Override
public void stop() {
status = ConnectorStatus.IDLE;
}

@Override
public void refresh() {
// no-op in template; implement sync logic in connectors
}

public void setCheckpoint(I_Checkpoint checkpoint) {
this.checkpoint = Optional.ofNullable(checkpoint);
}
}
