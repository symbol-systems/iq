---
title: Monitoring and Observability
audience: ["devops", "operator", "architect"]
sections: ["health", "metrics", "logging", "alerting", "tracing"]
---

# Monitoring and Observability Guide

Monitor IQ system health, performance, and costs.

## Health Checks

### API Health Endpoint

```bash
# Simple health check
curl http://localhost:8080/health

# Response:
{
  "status": "UP",
  "components": {
"database": { "status": "UP" },
"diskSpace": { "status": "UP", "details": { "free": "45GB" } },
"livenessState": { "status": "UP" },
"readinessState": { "status": "UP" }
  }
}
```

### Detailed Health Check

```bash
curl http://localhost:8080/health/details

# Response includes detailed component status:
{
  "status": "UP",
  "components": {
"database": {
  "status": "UP",
  "details": {
"repository": "prod-repo",
"type": "RemoteRepository",
"connected": true,
"latencyMs": 23
  }
},
"kubernetes": {
  "status": "UP",
  "details": {
"cluster": "us-east-1",
"nodes": 3,
"connectorStatus": "HEALTHY"
  }
},
"slack": {
  "status": "DEGRADED",
  "details": {
"message": "Rate limit approaching",
"requestsRemaining": 5
  }
}
  }
}
```

### Automated Health Checks

```bash
# CLI health check
./bin/iq-cli-server health --detailed

# Cluster health
./bin/iq-cli-server cluster list

# Repository health
./bin/iq-cli repositories verify prod-repo --quick

# Connector health
./bin/iq-cli connectors list | grep -E "Status|Synced"
```

## Metrics and Dashboards

### Prometheus Metrics

IQ exposes Prometheus metrics at `/metrics`:

```bash
curl http://localhost:8080/metrics

# Output (sample):
# TYPE iq_query_duration_seconds histogram
iq_query_duration_seconds_bucket{le="0.01",realm="prod"} 234
iq_query_duration_seconds_bucket{le="0.1",realm="prod"} 1045
iq_query_duration_seconds_bucket{le="1.0",realm="prod"} 2341
iq_query_duration_seconds_count{realm="prod"} 2400
iq_query_duration_seconds_sum{realm="prod"} 145.3

# TYPE iq_triples_total gauge
iq_triples_total{realm="prod"} 5234891
iq_triples_total{realm="staging"} 1823456

# TYPE iq_connector_sync_duration_seconds histogram
iq_connector_sync_duration_seconds_count{connector="aws-prod"} 24
iq_connector_sync_duration_seconds_sum{connector="aws-prod"} 1234.5
```

### Key Metrics

| Metric | Type | Alert Threshold |
|--------|------|-----------------|
| `iq_api_requests_total` | Counter | - |
| `iq_api_latency_*` | Histogram | p99 > 1000ms |
| `iq_query_duration_*` | Histogram | p99 > 30s |
| `iq_triples_total` | Gauge | - |
| `iq_repository_size_bytes` | Gauge | >500GB |
| `iq_connector_sync_duration_*` | Histogram | > 600s |
| `iq_llm_tokens_total` | Counter | - |
| `iq_llm_cost_total_dollars` | Gauge | >5000/month |
| `iq_cache_hit_ratio` | Gauge | < 0.7 |
| `iq_cluster_nodes_healthy` | Gauge | < quorum size |

### Grafana Dashboard

Create dashboard in Grafana:

```json
{
  "dashboard": {
"title": "IQ System Monitoring",
"panels": [
  {
"title": "API Request Rate",
"targets": [
  {
"expr": "rate(iq_api_requests_total[5m])"
  }
]
  },
  {
"title": "Query Latency (p99)",
"targets": [
  {
"expr": "histogram_quantile(0.99, iq_query_duration_seconds)"
  }
]
  },
  {
"title": "Repository Size",
"targets": [
  {
"expr": "iq_repository_size_bytes / 1024 / 1024 / 1024"
  }
]
  },
  {
"title": "Connector Sync Duration",
"targets": [
  {
"expr": "histogram_quantile(0.95, iq_connector_sync_duration_seconds)"
  }
]
  },
  {
"title": "LLM Cost Tracking",
"targets": [
  {
"expr": "iq_llm_cost_total_dollars"
  }
]
  }
]
  }
}
```

## Logging

### Configure Logging

Create `.iq/log4j2.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="io.quarkus.logging">
  <Appenders>
<Console name="console" target="SYSTEM_OUT">
  <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5p %c - %m%n"/>
</Console>

<RollingFile name="file" fileName="logs/iq.log" filePattern="logs/iq-%d{yyyy-MM-dd}-%i.log">
  <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5p %c - %m%n"/>
  <Policies>
<TimeBasedTriggeringPolicy interval="1" modulate="true"/>
<SizeBasedTriggeringPolicy size="100MB"/>
  </Policies>
  <DefaultRolloverStrategy max="30"/>
</RollingFile>

<RollingFile name="iq-queries" fileName="logs/queries.log" filePattern="logs/queries-%d{yyyy-MM-dd}.log">
  <PatternLayout pattern="%d{HH:mm:ss.SSS} %m%n"/>
  <Policies>
<TimeBasedTriggeringPolicy interval="1"/>
  </Policies>
</RollingFile>
  </Appenders>
  
  <Loggers>
<Logger name="systems.symbol" level="INFO">
  <AppenderRef ref="file"/>
</Logger>
<Logger name="systems.symbol.sparql" level="DEBUG">
  <AppenderRef ref="iq-queries"/>
</Logger>
<Logger name="systems.symbol.connector" level="INFO">
  <AppenderRef ref="file"/>
</Logger>
<Root level="INFO">
  <AppenderRef ref="console"/>
  <AppenderRef ref="file"/>
</Root>
  </Loggers>
</Configuration>
```

### Log Levels

```bash
# Set log level at runtime
export IQ_LOG_LEVEL="DEBUG"

# Or via CLI
./bin/iq-cli-server log-level set systems.symbol.sparql DEBUG

# View logs
tail -f "${IQ_HOME}/logs/iq.log"

# Filter by level
grep ERROR "${IQ_HOME}/logs/iq.log"

# Filter by component
grep "connector" "${IQ_HOME}/logs/iq.log"
```

## Distributed Tracing

Enable request tracing with Jaeger:

```bash
export QUARKUS_JAEGER_ENABLED=true
export QUARKUS_JAEGER_ENDPOINT="http://jaeger:14268/api/traces"
export QUARKUS_JAEGER_SERVICE_NAME="iq-api"
```

Query traces for specific request:

```bash
curl "http://jaeger:16686/api/traces?service=iq-api&operationName=sparql-query"
```

## Alerting Rules

### Prometheus Alert Rules

Create `iq-alerts.yml`:

```yaml
groups:
  - name: iq_alerts
rules:
  # High Query Latency
  - alert: HighQueryLatency
expr: histogram_quantile(0.99, iq_query_duration_seconds) > 30
for: 5m
annotations:
  summary: "High SPARQL query latency ({{ $value }}s)"

  # Repository Growing Too Fast
  - alert: RepositorySizeWarning
expr: iq_repository_size_bytes > 536870912000  # 500GB
for: 1h
annotations:
  summary: "Repository size approaching limit ({{ humanize $value }})"

  # Connector Sync Failures
  - alert: ConnectorSyncFailed
expr: iq_connector_sync_errors_total > 0
for: 10m
annotations:
  summary: "Connector {{ $labels.connector }} sync failures detected"

  # LLM Budget Exceeded
  - alert: LLMBudgetExceeded
expr: iq_llm_cost_total_dollars > 5000
for: 30m
annotations:
  summary: "Monthly LLM cost exceeded $5000 (current: ${{ $value }})"

  # Cluster Node Down
  - alert: ClusterNodeDown
expr: iq_cluster_nodes_healthy < 2
for: 2m
annotations:
  summary: "IQ cluster lost quorum ({{ $value }} healthy nodes)"

  # Cache Hit Ratio Low
  - alert: LowCacheHitRatio
expr: iq_cache_hit_ratio < 0.7
for: 15m
annotations:
  summary: "Cache hit ratio below target ({{ $value }})"
```

### Slack Notifications

```yaml
global:
  slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

route:
  receiver: 'slack-notifications'
  group_by: ['alertname', 'cluster']

receivers:
  - name: 'slack-notifications'
slack_configs:
  - channel: '#iq-alerts'
title: 'IQ Alert'
text: '{{ .GroupLabels.alertname }}: {{ .Alerts.Firing | len }} active'
actions:
  - type: button
text: 'View Dashboard'
url: 'http://grafana:3000/d/iq-monitoring'
color: '{{ if eq .GroupLabels.severity \"critical\" }}danger{{ else }}warning{{ end }}'
```

## Cost Tracking

### LLM Token Usage

```bash
# View detailed token usage
./bin/iq-cli metrics llm --period "last 30 days"

# Output:
# Provider  TokensCost
# openai:gpt-41,234,567 $45.67
# openai:gpt-3.5-turbo  456,789 $0.68
# groq:mixtral  789,012 $7.89
# Total  2,480,368 $54.24
```

### Cost Per Realm

```bash
# Track costs by realm
./bin/iq-cli metrics cost-by-realm --from "2024-01-01" --to "2024-01-31"

# Output:
# Realm LLM CostQuery Cost  Total
# prod $45.23  $2.34   $47.57
# staging  $3.45   $0.56   $4.01
# dev  $1.00   $0.23   $1.23
```

### Cost Alerts

Configure cost limits:

```turtle
iq:cost-policy
a iq:CostPolicy ;
iq:monthly_budget 5000 ;
iq:budget_period "2024-02-01" ;
iq:alert_at_percentage 75 ;
iq:hard_limit_percentage 100 ;
iq:enforcement "alert" ;  # or "block"
iq:notification_email "billing@example.com" .
```

## Capacity Planning

### Repository Growth

```bash
# Track growth over time
watch -n 3600 "date; ./bin/iq-cli repositories stat prod-repo | grep Triples"

# Export for analysis
for d in {1..30}; do
  echo "$(date +%Y-%m-$d): $(./bin/iq-cli repositories stat prod-repo --ts $(date -d "2024-01-$d" +%s) | grep Triples | awk '{print $NF}')"
done > /tmp/repo-growth.txt
```

### Query Performance Trends

```bash
# Get query latency percentiles
./bin/iq-cli metrics sparql --period "last 7 days" --percentiles "50,75,95,99"

# Output shows if latency is degrading
# Day p50 p75 p95 p99
# 2024-01-15  23ms45ms234ms   1203ms
# 2024-01-16  25ms48ms245ms   1156ms  << Improving
# 2024-01-17  21ms42ms198ms   987ms
```

## Performance Profiling

### Profile JVM

```bash
# Enable JFR (Java Flight Recorder)
export JVM_ARGS="-XX:StartFlightRecording=duration=60s,filename=/tmp/iq.jfr"

# Start server
./bin/iq

# Analyze recording
jfr dump --output=/tmp/iq-analysis.html /tmp/iq.jfr
open /tmp/iq-analysis.html
```

### Query Profiling

```bash
# Enable query plans
./bin/iq-cli sparql --explain --analyze "SELECT ?s WHERE { ?s ?p ?o } LIMIT 1000"

# Output shows:
# - Query plan (how RDF4J will execute it)
# - Estimated cardinality (number of results expected)
# - Actual execution time
# - Index usage
```

## SLA Monitoring

Define Service Level Objectives:

```yaml
objectives:
  - metric: api_latency_p99
threshold: 1000ms
target: 99%
name: "High-speed queries"
  
  - metric: api_availability
threshold: 99.9%
name: "High availability"
  
  - metric: data_sync_lag
threshold: 300s
name: "Replication freshness"
  
  - metric: cost_overage
threshold: 10%  # 10% over budget
name: "Cost control"
```

## Custom Metrics

Export custom metrics via Prometheus:

```java
// In your code
@Inject MeterRegistry meterRegistry;

public void doWork() {
  Timer.Sample sample = Timer.start(meterRegistry);
  
  // ... do work ...
  
  sample.stop(
  Timer.builder("custom.work.duration")
  .description("Duration of custom work")
  .tag("realm", "prod")
  .register(meterRegistry)
  );
  
  meterRegistry.counter("custom.operations", "type", "read").increment();
}
```

## Next Steps

1. **[Operations](10-OPERATIONS.md)** — Daily operational tasks
2. **[Clustering](08-CLUSTERING.md)** — Monitor cluster health
3. **[Troubleshooting](12-TROUBLESHOOTING.md)** — Debugging issues
