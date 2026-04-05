---
title: RDF Repository Types and Configuration
audience: ["devops", "architect"]
sections: ["types", "configuration", "migration", "tuning"]
---

# RDF Repository Guide

Understanding and configuring RDF4J and alternative RDF backends.

## Repository Types Comparison

| Type | Best For | Scalability | Latency | Redundancy | Cost |
|------|----------|-------------|---------|-----------|------|
| **Native** | Single node, dev/test | Small-medium <10M | Low | Basic | None |
| **Remote** | Multi-instance, shared | Large >10M | Medium | ✓ Full | Server cost |
| **FedX** | Distributed queries | Very large | Medium-high | ✓ Per-endpoint | Infrastructure |
| **GraphDB** | Inference, enterprise | Medium-large | Medium | ✓ Optional | License |
| **Custom SPARQL** | Wrap existing systems | Depends | Depends | Depends | Varies |

## 1. Native Repository (Default)

Embedded RDF4J with local filesystem storage.

### Setup

```turtle
@prefix iq: <urn:iq:> .

iq:repo-default
a iq:NativeRepository ;
iq:repositoryID "default" ;
iq:basePath "./repositories/default" ;
iq:persistent true ;
iq:indexed true ;
iq:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory" ;
iq:cacheSize 10000 ;
iq:tripleIndexes ("spoc" "posc" "cosp") ;
iq:valueIndex true ;
iq:contextIndex true .
```

### Directory Structure

```
./repositories/default/
├── contexts.dat# Context index
├── namespaces.dat  # Namespace mappings
├── triples-spoc.dat# Subject-Predicate-Object-Context
├── triples-spoc.alloc  # Allocation metadata
├── triples-posc.dat# Predicate-Object-Subject-Context
├── triples-posc.alloc
├── values.dat  # Literal and URI values
├── values.id   # Value identifiers
├── values.hash # Hash index for values
├── nativerdf.ver   # Format version
└── txn-status  # Transaction tracking
```

### Performance Tuning

```turtle
iq:repo-default-tuned
a iq:NativeRepository ;
iq:repositoryID "default" ;
iq:basePath "./repositories/default" ;
iq:persistent true ;
iq:indexed true ;
iq:cacheSize 50000 ;  # Increase from 10K
iq:tripleIndexes ("spoc" "posc" "cosp") ;  # Add more indexes
iq:valueIndex true ;
iq:contextIndex true ;
iq:fsyncEnabled false ;# Disable for better write perf
iq:bloomFilters true ; # Faster lookups
iq:bitmapIndex true ;  # Bitmap compression.
```

### Memory Settings

```bash
# Increase heap for large graphs
export JAVA_OPTS="-Xmx8g -XX:+UseG1GC"

# In startup script
JAVA_OPTS="-Xmx16g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:+ParallelRefProcEnabled"
```

### Limits

- **Triples**: 100 million+ (with tuning)
- **Concurrency**: ~20 concurrent queries
- **Latency**: 10-100ms for typical queries
- **Backup time**: 5-30 minutes (depending on size)

## 2. Remote Repository (RDF4J Server)

Share an RDF4J instance across multiple IQ nodes or external applications.

### Deploy RDF4J Server

```bash
# Download RDF4J Server
wget https://www.rdf4j.org/download/releases/4.3.9/eclipse-rdf4j-4.3.9.tar.gz
tar xzf eclipse-rdf4j-4.3.9.tar.gz
cd eclipse-rdf4j-4.3.9

# Start server
./bin/rdf4j-server
# Runs on http://localhost:8080 by default
```

### Configuration in IQ

```turtle
@prefix iq: <urn:iq:> .

iq:repo-shared
a iq:RemoteRepository ;
iq:repositoryID "shared-prod" ;
iq:serverURL "http://rdf4j-prod.internal:8080/rdf4j-server" ;
iq:username "iq_user" ;
iq:password "${secret:rdf4j/password}" ;
iq:readTimeout 30 ;
iq:writeTimeout 60 ;
iq:maxConnections 10 ;
iq:connectionPoolSize 20 ;
iq:retryOnConnectionFailure true ;
iq:maxRetries 3 ;
iq:backoffMultiplier 2.0 ;
iq:circuitBreakerEnabled true ;
iq:circuitBreakerThreshold 5 .
```

### Create Repository on Server

```bash
# Via REST API
curl -X PUT http://rdf4j-prod:8080/rdf4j-server/repositories/shared-prod \
  -H "Content-Type: application/x-turtleconfig" \
  -d '
  @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
  @prefix rep: <http://www.openrdf.org/config/repository#> .
  @prefix sr: <http://www.openrdf.org/config/repository/sail#> .
  @prefix sail: <http://www.openrdf.org/config/sail#> .
  @prefix ns: <http://www.openrdf.org/config/sail/nativerdf#> .

  [] a rep:Repository ;
rep:repositoryID "shared-prod" ;
rdfs:label "Production Shared Repository" ;
rep:repositoryImpl [
  rep:repositoryType "native" ;
  sr:sailImpl [
sail:sailType "native" ;
ns:tripleIndexes "spoc,posc,cosp" ;
ns:valueIndex true
  ]
] .
  '

# List repositories
curl http://rdf4j-prod:8080/rdf4j-server/repositories
```

### HA Setup with Replication

```bash
# Node 1 (Primary)
export RDF4J_REPLICA_MODE="primary"
export RDF4J_REPLICA_SERVER="rdf4j-secondary:8080"

# Node 2 (Secondary)
export RDF4J_REPLICA_MODE="secondary"
export RDF4J_REPLICA_MASTER="http://rdf4j-primary:8080/rdf4j-server/repositories/shared-prod"
```

Configure in IQ:

```turtle
iq:repo-ha-primary
a iq:RemoteRepository ;
iq:repositoryID "shared-prod" ;
iq:serverURL "http://rdf4j-primary.internal:8080/rdf4j-server" ;
iq:replication [
iq:mode "active-replay" ;
iq:secondaries ("rdf4j-secondary1:8080" "rdf4j-secondary2:8080") ;
iq:syncInterval 1000
] .
```

## 3. FedX Federated Repository

Query across multiple distributed RDF endpoints without copying data.

```turtle
@prefix iq: <urn:iq:> .

iq:repo-federated
a iq:FedXRepository ;
iq:repositoryID "federated-prod" ;
iq:members (
iq:endpoint-aws-kg
iq:endpoint-gcp-kg
iq:endpoint-snowflake-kg
) ;
iq:optimizer "dynamic" ;
iq:prefixDeref true ;
iq:pruningEnabled true ;
iq:evaluation "strict" ;
iq:joinOrdering "cardinality" ;
iq:statistics true ;
iq:cacheResults true ;
iq:cacheTTL 3600 .

# AWS Knowledge Graph
iq:endpoint-aws-kg
a iq:SPARQLEndpoint ;
iq:name "AWS Knowledge Graph" ;
iq:url "http://knowledge.aws.internal/sparql" ;
iq:timeout 60 ;
iq:pageSize 10000 ;
iq:credentials iq:creds-aws-sparql .

iq:creds-aws-sparql
a iq:SPARQLCredentials ;
iq:username "fedx_user" ;
iq:password "${secret:aws/sparql-password}" .

# GCP Knowledge Graph
iq:endpoint-gcp-kg
a iq:SPARQLEndpoint ;
iq:name "GCP Knowledge Graph" ;
iq:url "http://knowledge.gcp.internal/sparql" ;
iq:timeout 60 ;
iq:pageSize 10000 ;
iq:credentials iq:creds-gcp-sparql .

# Snowflake (via semantic middleware)
iq:endpoint-snowflake-kg
a iq:SPARQLEndpoint ;
iq:name "Snowflake Data" ;
iq:url "https://snowflake.internal/rdf/sparql" ;
iq:timeout 90 ;
iq:pageSize 5000 .
```

### Performance Optimization

```bash
# Enable FedX statistics
export FEDX_STATISTICS_ENABLED=true

# Adjust join order
export FEDX_JOIN_ORDERING="cardinality"

# Enable caching
export FEDX_CACHE_ENABLED=true
export FEDX_CACHE_TTL=3600

# Parallel execution
export FEDX_PARALLEL_JOINS=true
export FEDX_PARALLEL_UNION=true
```

## 4. GraphDB Enterprise Repository

For semantic reasoning and inference.

```turtle
@prefix iq: <urn:iq:> .

iq:repo-graphdb
a iq:GraphDBRepository ;
iq:repositoryID "enterprise-kg" ;
iq:serverURL "http://graphdb-prod.internal:7200" ;
iq:username "admin" ;
iq:password "${secret:graphdb/password}" ;
iq:reasoner "owl-max" ;
iq:inferenceEnabled true ;
iq:baseURL "http://knowledge.example.com/" ;
iq:includeInferred true ;
iq:validationEnabled true ;
iq:sameasEnabled true ;
iq:redundancyOptimization true .
```

## Backup and Recovery

### Automated Backups

```turtle
iq:repo-default-backup
a iq:NativeRepository ;
iq:repositoryID "default" ;
iq:basePath "./repositories/default" ;
iq:backup [
iq:enabled true ;
iq:schedule "0 2 * * *" ;  # 2AM daily
iq:backupPath "./backups" ;
iq:retention 30 ;  # Keep 30 days
iq:compression "gzip" ;
iq:verify true
] .
```

### Manual Backup

```bash
# Backup Native Repository
tar -czf "${IQ_HOME}/backups/repo-$(date +%Y%m%d-%H%M%S).tar.gz" \
  --exclude="${IQ_HOME}/repositories/default/transaction" \
  "${IQ_HOME}/repositories/default"

# Backup Remote Repository (via REST)
curl -X POST \
  "http://rdf4j-prod:8080/rdf4j-server/repositories/shared-prod/statements?output=trig" \
  > "/backups/repo-export-$(date +%Y%m%d).trig"

# Export as SPARQL 1.1 CONSTRUCT
curl -G "http://rdf4j-prod:8080/sparql" \
  --data-urlencode 'query=CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }' \
  > "/backups/complete-graph-$(date +%Y%m%d).nq"
```

### Recovery

```bash
# Restore Native Repository
cd "${IQ_HOME}/repositories"
tar -xzf /backups/repo-backup.tar.gz

# Stop server, restore, restart
./bin/iq-cli-server server api stop
tar -xzf /backups/repo-backup.tar.gz -C repositories/
./bin/iq-cli-server server api start

# Verify
./bin/iq-cli sparql "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }"
```

## Migration

Migrate between repository types.

### Native → Remote Repository

```bash
# 1. Export from native
./mvnw -pl iq-platform exec:java \
  -Dexec.mainClass="systems.symbol.rdf.RDFExporter" \
  -Dexec.args="./repositories/default /tmp/export.nq"

# 2. Verify export
wc -l /tmp/export.nq

# 3. Create remote repo on RDF4J server
curl -X PUT http://rdf4j-prod:8080/rdf4j-server/repositories/new-repo \
  -H "Content-Type: application/x-turtleconfig" \
  -d '@config.ttl'

# 4. Import into remote
curl -X POST -H "Content-Type: application/rdf+ngquads" \
  --data-binary @/tmp/export.nq \
  "http://rdf4j-prod:8080/rdf4j-server/repositories/new-repo/statements"

# 5. Verify count
curl "http://rdf4j-prod:8080/rdf4j-server/repositories/new-repo/statements?limit=0"

# 6. Update config.ttl to point to remote
# iq:repository iq:repo-remote

# 7. Restart IQ
```

### FedX Federation Setup

```bash
# Create native repos for each endpoint
mkdir -p repositories/{aws,gcp,snowflake}

# Export to each endpoint
./mvnw exec:java \
  -Dexec.args="./repositories/default /tmp/aws-data.nq FILTER:subject:aws:*"

# Load into respective RDF4J servers
for endpoint in aws gcp snowflake; do
  curl -X POST -H "Content-Type: application/rdf+ngquads" \
--data-binary @"/tmp/${endpoint}-data.nq" \
"http/${endpoint}-rdf4j:8080/rdf4j-server/repositories/${endpoint}-repo/statements"
done

# Create FedX config pointing to all three
# iq:repo-federated with iq:members pointing to all endpoints
```

## Monitoring and Metrics

### Repository Statistics

```bash
# Query graph size
./bin/iq-cli sparql "SELECT (COUNT(*) AS ?triples) WHERE { ?s ?p ?o }"

# Query latency
./bin/iq-cli sparql --timing "SELECT ?s WHERE { ?s ?p ?o } LIMIT 1000"

# Connection pool status
./bin/iq-cli repositories status

# Cache hit rate (Remote)
./bin/iq-cli metrics repositories remote cache-hit-rate
```

### Performance Benchmarks

```bash
# Benchmark read performance
time ./bin/iq-cli sparql \
  "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 100000"

# Benchmark write performance
time ./bin/iq-cli sparql --update \
  < insert-1m-triples.sparql

# Check backup time
time tar -czf repo-backup.tar.gz repositories/default
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `OutOfMemory` | Increase heap, tune indexes |
| `Slow queries` | Add indexes, check query plan |
| `Connection refused` | Check server URL, firewall, credentials |
| `Inference timeout` | Reduce rule complexity, increase timeout |
| `Sync lag (FedX)` | Check endpoint availability, network |

## Next Steps

1. **[Connectors](05-CONNECTORS.md)** — Load data into repositories
2. **[Monitoring](11-MONITORING.md)** — Health checks and metrics
3. **[Operations](10-OPERATIONS.md)** — Day-to-day management
