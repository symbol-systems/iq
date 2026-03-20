package systems.symbol.connect.core;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Base connector implementation that centralizes connector lifecycle boilerplate.
 *
 * <p>Subclasses implement {@link #doRefresh()} to perform source-specific sync logic.
 */
public abstract class AbstractConnector implements I_Connector, I_ConnectorDescriptor {

    private final IRI connectorId;
    private final Model state;
    private final IRI graphIri;
    private final IRI ontologyBaseIri;
    private final IRI entityBaseIri;

    private volatile ConnectorStatus status = ConnectorStatus.IDLE;
    private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

    protected AbstractConnector(String connectorId,
                                Model state,
                                IRI graphIri,
                                IRI ontologyBaseIri,
                                IRI entityBaseIri) {
        this(Values.iri(Objects.requireNonNull(connectorId, "connectorId")),
            state,
            graphIri,
            ontologyBaseIri,
            entityBaseIri);
    }

    protected AbstractConnector(IRI connectorId,
                                Model state,
                                IRI graphIri,
                                IRI ontologyBaseIri,
                                IRI entityBaseIri) {
        this.connectorId = Objects.requireNonNull(connectorId, "connectorId");
        this.state = Objects.requireNonNull(state, "state");
        this.graphIri = Objects.requireNonNull(graphIri, "graphIri");
        this.ontologyBaseIri = Objects.requireNonNull(ontologyBaseIri, "ontologyBaseIri");
        this.entityBaseIri = Objects.requireNonNull(entityBaseIri, "entityBaseIri");
    }

    @Override
    public final IRI getSelf() {
        return connectorId;
    }

    @Override
    public final IRI getConnectorId() {
        return connectorId;
    }

    @Override
    public final ConnectorStatus getStatus() {
        return status;
    }

    @Override
    public final Model getModel() {
        return state;
    }

    @Override
    public final Optional<I_Checkpoint> getCheckpoint() {
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
    public final void refresh() {
        status = ConnectorStatus.SYNCING;
        state.remove(null, null, null, graphIri);
        ConnectorSyncMetadata.markSyncing(state, connectorId, graphIri);

        IRI activity = ConnectorProvenance.markSyncStarted(state, connectorId, graphIri);
        try {
            doRefresh();
            checkpoint = Objects.requireNonNullElseGet(createCheckpoint(), Optional::empty);
            status = ConnectorStatus.IDLE;
            ConnectorSyncMetadata.markSynced(state, connectorId, graphIri);
            ConnectorProvenance.markSyncCompleted(state, activity, graphIri);
        } catch (Exception e) {
            status = ConnectorStatus.ERROR;
            ConnectorSyncMetadata.markError(state, connectorId, graphIri);
            ConnectorProvenance.markSyncFailed(state, activity, e, graphIri);
        }
    }

    /**
     * Runs one connector refresh cycle.
     */
    protected abstract void doRefresh() throws Exception;

    /**
     * Produces a checkpoint after a successful refresh.
     */
    protected Optional<I_Checkpoint> createCheckpoint() {
        return Optional.of(Checkpoints.of(state));
    }

    protected final IRI graphIri() {
        return graphIri;
    }

    protected final IRI ontologyBaseIri() {
        return ontologyBaseIri;
    }

    protected final IRI entityBaseIri() {
        return entityBaseIri;
    }
}