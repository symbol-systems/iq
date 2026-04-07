package systems.symbol.connect.core;

import java.util.Objects;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;

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

// Metrics counters
private static final Counter.Builder syncCounterBuilder = Counter.builder("connector.sync.total")
.description("Total number of connector sync operations");
private static final Counter.Builder successCounterBuilder = Counter.builder("connector.sync.success")
.description("Number of successful connector sync operations");
private static final Counter.Builder errorCounterBuilder = Counter.builder("connector.sync.error")
.description("Number of failed connector sync operations");

private volatile Counter syncCounter;
private volatile Counter successCounter;
private volatile Counter errorCounter;
private volatile Tracer tracer;
private volatile MeterRegistry meterRegistry;

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
@WithSpan(value = "connector.refresh")
public final void refresh() {
// Initialize metrics on first use (lazy initialization)
if (syncCounter == null && meterRegistry != null) {
initializeMetrics();
}

// Increment total sync counter
if (syncCounter != null) {
syncCounter.increment();
}

// Add span attributes for better observability
Span currentSpan = Span.current();
if (currentSpan != null && currentSpan.isRecording()) {
currentSpan.setAttribute("connector.id", connectorId.stringValue());
currentSpan.setAttribute("graph.iri", graphIri.stringValue());
currentSpan.setAttribute("connector.mode", getMode().toString());
}

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

// Increment success counter
if (successCounter != null) {
successCounter.increment();
}
if (currentSpan != null && currentSpan.isRecording()) {
currentSpan.setAttribute("sync.status", "success");
}
} catch (Exception e) {
status = ConnectorStatus.ERROR;
ConnectorSyncMetadata.markError(state, connectorId, graphIri);
ConnectorProvenance.markSyncFailed(state, activity, e, graphIri);

// Increment error counter
if (errorCounter != null) {
errorCounter.increment();
}
if (currentSpan != null && currentSpan.isRecording()) {
currentSpan.setAttribute("sync.status", "error");
currentSpan.setAttribute("sync.error", e.getClass().getSimpleName());
currentSpan.setAttribute("sync.error.message", e.getMessage() != null ? e.getMessage() : "");
}
}
}

/**
 * Initializes metrics counters with the provided MeterRegistry.
 */
private synchronized void initializeMetrics() {
if (syncCounter == null && meterRegistry != null) {
syncCounter = syncCounterBuilder
.tag("connector.id", connectorId.stringValue())
.register(meterRegistry);
successCounter = successCounterBuilder
.tag("connector.id", connectorId.stringValue())
.register(meterRegistry);
errorCounter = errorCounterBuilder
.tag("connector.id", connectorId.stringValue())
.register(meterRegistry);
}
}

/**
 * Sets the MeterRegistry for metrics collection. Can be called by dependency injection frameworks.
 */
public void setMeterRegistry(MeterRegistry meterRegistry) {
this.meterRegistry = meterRegistry;
}

/**
 * Sets the OpenTelemetry Tracer for distributed tracing. Can be called by dependency injection frameworks.
 */
public void setTracer(Tracer tracer) {
this.tracer = tracer;
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