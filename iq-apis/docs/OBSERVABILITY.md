# IQ Platform Observability Guide

This document describes the observability features added to IQ including distributed tracing and metrics export.

## Distributed Tracing (M-9)

### Overview
IQ now instruments key components with OpenTelemetry to provide end-to-end trace visibility across:
- LLM requests (OpenAI, Groq, etc.)
- SPARQL queries (federated and local)
- Connector sync operations (AWS, GitHub, Slack, etc.)
- Agent FSM state transitions
- RDF repository operations

### Configuration

Tracing is configured in `iq-apis/src/main/resources/application.properties`:

```properties
quarkus.otel.enabled=true
quarkus.otel.traces.exporter=otlp
quarkus.otel.service.name=iq-apis
quarkus.otel.service.version=0.94.1
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.traces.sampler=parentbased_always_on
quarkus.otel.traces.sampler.arg=0.9
```

### Backend Setup

#### Option 1: Docker Compose with Jaeger + OTLP Collector

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  otel-collector:
image: otel/opentelemetry-collector:latest
ports:
  - "4317:4317"  # OTLP gRPC
  - "4318:4318"  # OTLP HTTP
environment:
  - OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger:14250
volumes:
  - ./otel-config.yaml:/etc/otel/config.yaml
command: --config=/etc/otel/config.yaml

  jaeger:
image: jaegertracing/all-in-one:latest
ports:
  - "6831:6831/udp"
  - "16686:16686"  # Jaeger UI
environment:
  - COLLECTOR_OTLP_ENABLED=true

  prometheus:
image: prom/prometheus:latest
ports:
  - "9090:9090"
volumes:
  - ./prometheus.yml:/etc/prometheus/prometheus.yml
```

Then run:
```bash
docker-compose up -d
```

Access Jaeger UI at `http://localhost:16686`

#### Option 2: Kubernetes with OpenTelemetry Operator

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm install opentelemetry-operator open-telemetry/opentelemetry-operator
```

## Metrics Export (M-10)

### Overview
IQ exports real-time metrics via Micrometer and Prometheus:
- Connector sync success/failure rates
- LLM token consumption and estimated costs
- RDF triple counts per named graph
- Agent FSM state counters

### Configuration

Metrics are enabled by default and exported on `/metrics` endpoint:

```properties
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
```

### Accessing Metrics

#### Prometheus Endpoint
```
http://localhost:8080/metrics
```

Full metrics output in Prometheus format.

#### Key Metrics

| Metric | Description | Type |
|--------|-------------|------|
| `connector.sync.total` | Total sync operations | Counter |
| `connector.sync.success` | Successful syncs | Gauge |
| `connector.sync.failure` | Failed syncs | Gauge |
| `llm.tokens.input.total` | Input tokens consumed | Gauge |
| `llm.tokens.output.total` | Output tokens produced | Gauge |
| `llm.requests.total` | Total LLM requests | Gauge |
| `llm.estimated.cost.microusd` | Estimated LLM cost (µUSD) | Gauge |
| `rdf.triples.total` | Total RDF triples | Gauge |
| `agent.state.transitions` | Agent FSM transitions | Counter |

### Prometheus Scrape Configuration

Add to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'iq-apis'
static_configs:
  - targets: ['localhost:8080']
metrics_path: '/metrics'
scrape_interval: 15s
```

### Grafana Dashboards

Example dashboard JSON for visualizing IQ metrics:

```json
{
  "dashboard": {
"title": "IQ Platform Metrics",
"panels": [
  {
"title": "Connector Sync Success Rate",
"targets": [
  {
"expr": "rate(connector.sync.success[5m])"
  }
]
  },
  {
"title": "LLM Token Usage Rate",
"targets": [
  {
"expr": "rate(llm.tokens.input.total[5m])"
  }
]
  },
  {
"title": "Estimated LLM Costs",
"targets": [
  {
"expr": "increase(llm.estimated.cost.microusd[1h]) / 1000000"
  }
]
  }
]
  }
}
```

## Best Practices

### For Development
- Set `quarkus.otel.enabled=false` in development mode (already configured in `application-dev.properties`)
- Use local Jaeger or OTLP Collector in Docker
- Monitor metrics via Prometheus on `http://localhost:9090`

### For Production
- Use managed service for trace collection (e.g., Datadog, New Relic, AWS X-Ray)
- Set sampling rate appropriately via `OTEL_TRACES_SAMPLER_ARG` (0.1 = 10%)
- Use dedicated Prometheus instance with long retention
- Set up alerts for high failure rates and cost anomalies
- Export logs alongside traces for better correlation

## Troubleshooting

### Traces not appearing in backend
1. Check `OTEL_EXPORTER_OTLP_ENDPOINT` is reachable
2. Verify sampler is configured: `quarkus.otel.traces.sampler=parentbased_always_on`
3. Check logs for "IQMetricsService initialized" message
4. Ensure OTLP exporter dependency is in classpath

### High memory usage
- Reduce sampling rate: `quarkus.otel.traces.sampler.arg=0.1`
- Increase batch size: `OTEL_BATCHER_MAX_QUEUE_SIZE=512`

### Missing metrics
1. Access `/metrics` endpoint to verify metrics are exported
2. Check Prometheus scrape config and targets
3. Verify `quarkus.micrometer.enabled=true`

## References

- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/instrumentation/java/)
- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [Micrometer Documentation](https://micrometer.io/)
- [Prometheus Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
