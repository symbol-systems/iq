package systems.symbol.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized metrics service for IQ platform.
 * 
 * Exports metrics for:
 * - Connector sync operations (success/failure counts, rates)
 * - LLM token usage (input/output tokens, estimated costs)
 * - RDF repository stats (triples per named graph)
 * - Agent FSM state transitions
 * 
 * Integrates with Micrometer for Prometheus export.
 */
@Singleton
public class IQMetricsService {
private static final Logger log = LoggerFactory.getLogger(IQMetricsService.class);

private final MeterRegistry meterRegistry;

// Connector metrics
private final AtomicLong connectorSyncTotal = new AtomicLong(0);
private final AtomicLong connectorSyncSuccess = new AtomicLong(0);
private final AtomicLong connectorSyncFailure = new AtomicLong(0);
private final ConcurrentHashMap<String, AtomicLong> connectorFailures = new ConcurrentHashMap<>();

// LLM metrics
private final AtomicLong totalInputTokens = new AtomicLong(0);
private final AtomicLong totalOutputTokens = new AtomicLong(0);
private final AtomicLong totalLLMRequests = new AtomicLong(0);
private final AtomicLong estimatedCostMicroUSD = new AtomicLong(0);
private final ConcurrentHashMap<String, LLMProviderMetrics> llmProviderMetrics = new ConcurrentHashMap<>();

// RDF metrics
private final ConcurrentHashMap<String, AtomicLong> rdfGraphTripleCounts = new ConcurrentHashMap<>();
private final AtomicLong totalRDFTriples = new AtomicLong(0);

// Agent metrics
private final ConcurrentHashMap<String, AtomicLong> agentStateCounts = new ConcurrentHashMap<>();

public IQMetricsService(MeterRegistry meterRegistry) {
this.meterRegistry = meterRegistry;
registerMetrics();
}

/**
 * Register all gauges and counters with Micrometer.
 */
private void registerMetrics() {
// Connector sync metrics
Counter.builder("connector.sync.total")
.description("Total connector sync operations")
.register(meterRegistry)
.increment(0);

Gauge.builder("connector.sync.success", connectorSyncSuccess, AtomicLong::get)
.description("Successful connector sync operations")
.register(meterRegistry);

Gauge.builder("connector.sync.failure", connectorSyncFailure, AtomicLong::get)
.description("Failed connector sync operations")
.register(meterRegistry);

// LLM token metrics
Gauge.builder("llm.tokens.input.total", totalInputTokens, AtomicLong::get)
.description("Total input tokens consumed by LLM requests")
.register(meterRegistry);

Gauge.builder("llm.tokens.output.total", totalOutputTokens, AtomicLong::get)
.description("Total output tokens produced by LLM requests")
.register(meterRegistry);

Gauge.builder("llm.requests.total", totalLLMRequests, AtomicLong::get)
.description("Total number of LLM requests")
.register(meterRegistry);

Gauge.builder("llm.estimated.cost.microusd", estimatedCostMicroUSD, AtomicLong::get)
.description("Estimated LLM cost in micro-USD")
.register(meterRegistry);

// RDF metrics
Gauge.builder("rdf.triples.total", totalRDFTriples, AtomicLong::get)
.description("Total RDF triples in all named graphs")
.register(meterRegistry);

log.info("IQMetricsService initialized with Prometheus export enabled");
}

/**
 * Record a successful connector sync operation.
 */
public void recordConnectorSyncSuccess(String connectorId) {
connectorSyncTotal.incrementAndGet();
connectorSyncSuccess.incrementAndGet();
connectorFailures.remove(connectorId); // Clear failure count on success
}

/**
 * Record a failed connector sync operation.
 */
public void recordConnectorSyncFailure(String connectorId, String errorMessage) {
connectorSyncTotal.incrementAndGet();
connectorSyncFailure.incrementAndGet();
connectorFailures.computeIfAbsent(connectorId, k -> new AtomicLong(0)).incrementAndGet();
log.warn("Connector {} sync failed: {}", connectorId, errorMessage);
}

/**
 * Record LLM token usage and cost.
 */
public synchronized void recordLLMUsage(String provider, String model, long inputTokens, long outputTokens, double costUSD) {
totalInputTokens.addAndGet(inputTokens);
totalOutputTokens.addAndGet(outputTokens);
totalLLMRequests.incrementAndGet();
estimatedCostMicroUSD.addAndGet((long)(costUSD * 1_000_000));

LLMProviderMetrics metrics = llmProviderMetrics.computeIfAbsent(
provider + ":" + model,
k -> new LLMProviderMetrics(provider, model)
);
metrics.recordUsage(inputTokens, outputTokens, costUSD);
}

/**
 * Update RDF triple count for a named graph.
 */
public void updateRDFTripleCount(String graphName, long tripleCount) {
AtomicLong previous = rdfGraphTripleCounts.get(graphName);
long previousCount = (previous != null) ? previous.get() : 0;
rdfGraphTripleCounts.put(graphName, new AtomicLong(tripleCount));
totalRDFTriples.addAndGet(tripleCount - previousCount);
}

/**
 * Record agent FSM state transition.
 */
public void recordAgentStateTransition(String agentId, String newState) {
agentStateCounts.computeIfAbsent(newState, k -> new AtomicLong(0)).incrementAndGet();
}

/**
 * Get all connector failure counts.
 */
public Collection<AtomicLong> getConnectorFailures() {
return new ArrayList<>(connectorFailures.values());
}

/**
 * Get LLM provider metrics summary.
 */
public Collection<LLMProviderMetrics> getLLMProviderMetrics() {
return new ArrayList<>(llmProviderMetrics.values());
}

/**
 * Inner class for LLM provider cost tracking.
 */
public static class LLMProviderMetrics {
public String provider;
public String model;
public AtomicLong inputTokens = new AtomicLong(0);
public AtomicLong outputTokens = new AtomicLong(0);
public AtomicLong requestCount = new AtomicLong(0);
public AtomicLong estimatedCostMicroUSD = new AtomicLong(0);

public LLMProviderMetrics(String provider, String model) {
this.provider = provider;
this.model = model;
}

public void recordUsage(long inputTokens, long outputTokens, double costUSD) {
this.inputTokens.addAndGet(inputTokens);
this.outputTokens.addAndGet(outputTokens);
this.requestCount.incrementAndGet();
this.estimatedCostMicroUSD.addAndGet((long)(costUSD * 1_000_000));
}
}
}
