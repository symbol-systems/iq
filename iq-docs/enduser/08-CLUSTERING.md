---
title: Clustering and High Availability
audience: ["devops", "architect"]
sections: ["setup", "ha", "replication", "load-balancing", "disaster-recovery"]
---

# Clustering and High Availability Guide

Deploy IQ across multiple nodes for high availability, fault tolerance, and horizontal scaling.

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│   Load Balancer  │
│  (HAProxy / Nginx / ALB) │
└──────────────┬──────────┬──────────┬────────────────┘
   │  │  │
 ┌─────▼──┐  ┌────▼──┐  ┌───▼────┐
 │ IQ │  │  IQ   │  │  IQ│ (API nodes)
 │ API-1  │  │ API-2 │  │ API-3  │
 └────┬───┘  └────┬──┘  └───┬────┘
  │  │  │
  └──────────┬──────────┘
 │
  ┌──────────────┴──────────────┐
  ┌───▼────┐  ┌────▼────┐
  │ RDF4J  │  │ RDF4J   │ (Repository cluster)
  │ Primary├──Replication─┤Secondary│
  └────────┘  └────┬────┘
   │
  (optional 2°/3°)
```

## Minimal HA Setup (3 nodes)

The simplest production-ready deployment with fault tolerance for 1 node failure.

### Prerequisites

- 3 servers, each with:
  - 8GB+ RAM
  - 50GB+ disk
  - 1Gbps network
  - Java 21
  - Maven (or pre-built JARs)

### Hosts

```bash
# Define hostnames
node1.iq.internal  # IQ API + RDF4J Primary
node2.iq.internal  # IQ API + RDF4J Secondary
node3.iq.internal  # IQ API + Witness (RDF4J voting)
```

### Step 1: Install on All Nodes

```bash
# On each node
java -version  # Verify Java 21
git clone https://github.com/symbol-systems/iq.git
cd iq
./mvnw clean install -DskipITs=true
```

### Step 2: Configure RDF4J Cluster

**On node1 (Primary):**

```bash
cd /opt/rdf4j-server
./bin/rdf4j-server

# Create repository with replication
curl -X PUT http://node1:8080/rdf4j-server/repositories/prod-repo \
  -H "Content-Type: application/x-turtleconfig" \
  -d '
  @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
  @prefix rep: <http://www.openrdf.org/config/repository#> .
  @prefix sr: <http://www.openrdf.org/config/repository/sail#> .
  @prefix sail: <http://www.openrdf.org/config/sail#> .
  @prefix ns: <http://www.openrdf.org/config/sail/nativerdf#> .

  [] a rep:Repository ;
rep:repositoryID "prod-repo" ;
rep:repositoryImpl [
  rep:repositoryType "native" ;
  sr:sailImpl [
sail:sailType "native" ;
ns:tripleIndexes "spoc,posc,cosp"
  ]
] ;
rep:replicationMode "primary" ;
rep:replicas ("node2:8080" "node3:8080") ;
rep:syncInterval 1000 .
  '
```

**On node2 and node3 (Secondary/Witness):**

```bash
# Point to primary
export RDF4J_REPLICA_MASTER="http://node1:8080/rdf4j-server/repositories/prod-repo"
export RDF4J_REPLICATION_MODE="secondary"

./bin/rdf4j-server
```

Wait for sync:

```bash
# Check sync status
curl -s http://node2:8080/rdf4j-server/repositories/prod-repo/metadata | jq '.replicationStatus'
# Response: "SYNCED"
```

### Step 3: Configure IQ on All Nodes

Create `.iq/config.ttl`:

```turtle
@prefix iq: <urn:iq:> .

# Override for each node
iq:cluster-config
a iq:ClusterConfig ;
iq:nodeId "node1" ; # Change per node
iq:nodeAddress "node1.iq.internal:8081" ;
iq:peers (
"node2.iq.internal:8081"
"node3.iq.internal:8081"
) ;
iq:discoveryMethod "static" ;
iq:heartbeatInterval 5000 ;
iq:electionEnabled true ;
iq:quorumSize 2 .

iq:realm-prod-cluster
a iq:Realm ;
iq:name "prod" ;
iq:repository iq:repo-prod-remote ;
iq:llmConfig iq:llm-gpt4 ;
iq:clusterConfig iq:cluster-config .

iq:repo-prod-remote
a iq:RemoteRepository ;
iq:repositoryID "prod-repo" ;
iq:serverURL "http://localhost:8080/rdf4j-server" ;  # Local RDF4J
iq:username "iq_user" ;
iq:password "${secret:rdf4j/password}" ;
iq:maxConnections 20 ;
iq:retryOnConnectionFailure true ;
iq:maxRetries 3 .
```

### Step 4: Start IQ Cluster

```bash
# All nodes simultaneously (or sequentially with sync checks)

# Node 1
ssh node1 "cd /opt/iq && ./bin/iq"

# Node 2
ssh node2 "cd /opt/iq && ./bin/iq"

# Node 3
ssh node3 "cd /opt/iq && ./bin/iq"

# Verify cluster is formed
./bin/iq-cli-server cluster list
# Output:
# node1.iq.internal:8081  LEADER   SYNCED
# node2.iq.internal:8081  FOLLOWER SYNCED
# node3.iq.internal:8081  FOLLOWER SYNCED
```

### Step 5: Load Balancer

**HAProxy Configuration** (`/etc/haproxy/haproxy.cfg`):

```ini
global
log /dev/log local0
maxconn 4096
daemon

defaults
log global
modehttp
option  httplog
option  dontlognull
timeout connect 5000
timeout client  50000
timeout server  50000

frontend iq_frontend
bind *:8080
mode http
default_backend iq_backend
option forwardfor

backend iq_backend
mode http
balance roundrobin
option httpchk GET /health HTTP/1.1
http-check expect status 200

server node1 node1.iq.internal:8080 check inter 10s
server node2 node2.iq.internal:8080 check inter 10s
server node3 node3.iq.internal:8080 check inter 10s

# Sticky sessions (optional)
cookie SERVERID insert indirect nocache
server node1 node1.iq.internal:8080 cookie node1 check
server node2 node2.iq.internal:8080 cookie node2 check
server node3 node3.iq.internal:8080 cookie node3 check
```

Start HAProxy:

```bash
systemctl restart haproxy
curl http://localhost:8080/health
```

**Kubernetes Service** (if running in K8S):

```yaml
apiVersion: v1
kind: Service
metadata:
  name: iq-api
spec:
  selector:
app: iq-api
  type: LoadBalancer
  ports:
  - port: 8080
targetPort: 8080
protocol: TCP

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iq-api-cluster
spec:
  replicas: 3
  selector:
matchLabels:
  app: iq-api
  template:
metadata:
  labels:
app: iq-api
spec:
  containers:
  - name: iq-api
image: iq:latest
ports:
- containerPort: 8080
env:
- name: IQ
  valueFrom:
fieldRef:
  fieldPath: metadata.pod-uid
- name: IQ_CLUSTER_ENABLED
  value: "true"
- name: IQ_CLUSTER_PEERS
  value: "iq-api-0:8081,iq-api-1:8081,iq-api-2:8081"
livenessProbe:
  httpGet:
path: /health
port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
path: /health/ready
port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

## Scaling to 10+ Nodes

For very large deployments:

### Regional Deployment

```turtle
# Primary region (3-5 nodes)
iq:cluster-us-east-primary
a iq:ClusterRegion ;
iq:region "us-east-1" ;
iq:nodes (
"us-east-1a.iq.internal"
"us-east-1b.iq.internal"
"us-east-1c.iq.internal"
) ;
iq:replicationRole "primary" .

# Secondary region (2-3 nodes)
iq:cluster-eu-west-secondary
a iq:ClusterRegion ;
iq:region "eu-west-1" ;
iq:nodes (
"eu-west-1a.iq.internal"
"eu-west-1b.iq.internal"
) ;
iq:replicationRole "secondary" ;
iq:replicationSource iq:cluster-us-east-primary ;
iq:replicationLag 5000 .  # Max 5 seconds
```

### Cross-Region Replication

```bash
# On primary region (US)
export RDF4J_REPLICATION_TARGETS="eu-west-1.rdf4j:8080"
export RDF4J_REPLICATION_ASYNC=true

# On secondary region (EU)
# Replicates all write operations every 5 seconds
```

## Backup and Disaster Recovery

### Automated Backups

```turtle
iq:backup-policy
a iq:BackupPolicy ;
iq:enabled true ;
iq:schedule "0 2 * * *" ;   # 2 AM daily
iq:retention 30 ;   # Keep 30 days
iq:fullBackupDays 7 ;   # Weekly full backups
iq:incrementalDays 1 ;  # Daily incremental
iq:destination "s3://iq-backups/" ;
iq:compression "gzip" ;
iq:encryption "AES-256" ;
iq:verification true ;  # Verify after backup
iq:notification "ops@example.com" .
```

### Manual Backup

```bash
# Backup all nodes
for node in node1 node2 node3; do
  ssh $node "tar -czf /tmp/iq-backup-$(date +%s).tar.gz \
/opt/iq/repositories \
/opt/iq/vault \
/opt/iq/jwt"
  scp $node:/tmp/iq-backup-*.tar.gz /backups/
done

# Verify backup integrity
for f in /backups/iq-backup-*.tar.gz; do
  tar -tzf "$f" > /dev/null && echo "OK: $f" || echo "FAIL: $f"
done
```

### Recovery Procedure

**If Primary Node Fails:**

```bash
# 1. Remove failed node from cluster
./bin/iq-cli-server cluster remove node1.iq.internal

# 2. Provision new node with same ID
# 3. Restore from backup (optional)
tar -xzf /backups/iq-backup-latest.tar.gz -C /opt/iq/

# 4. Start new node (will auto-sync from secondary)
./bin/iq -Diq.cluster.recovery=true

# 5. Rejoin cluster
./bin/iq-cli-server cluster add node1.iq.internal:8081
```

**Full Cluster Recovery:**

```bash
# 1. Stop all nodes
for node in node1 node2 node3; do
  ssh $node "pkill -f 'java.*iq-apis'"
done

# 2. Restore from latest backup on all nodes
for node in node1 node2 node3; do
  ssh $node "tar -xzf /backups/iq-backup-latest.tar.gz -C /opt/iq/"
done

# 3. Remove cluster state to force rebuild
for node in node1 node2 node3; do
  ssh $node "rm -f /opt/iq/.iq/runtime/cluster.*"
done

# 4. Start cluster (primary first, then secondaries)
ssh node1 "./bin/iq"
sleep 30  # Wait for primary to be ready
ssh node2 "./bin/iq"
ssh node3 "./bin/iq"

# 5. Verify sync
./bin/iq-cli-server cluster list
```

## Monitoring Cluster Health

```bash
# Check cluster status
./bin/iq-cli-server cluster list

# Output:
# NodeIDRole   Status  Synced
# node1.iq.internal:8081LEADER HEALTHY SYNCED
# node2.iq.internal:8081FOLLOWER   HEALTHY SYNCED
# node3.iq.internal:8081FOLLOWER   HEALTHY SYNCED

# Check individual node
./bin/iq-cli-server node-status node1.iq.internal

# Monitor replication lag
./bin/iq-cli-server replication-lag

# Check election state
./bin/iq-cli-server election status
```

## Failure Scenarios

### Node Failure

| Scenario | Impact | Recovery |
|----------|--------|----------|
| **1 of 3 fails** | No impact, system continues | Node auto-removed, healthy nodes continue |
| **2 of 3 fail** | Cluster loses quorum, reads fail | Stop remaining node, fix nodes, restart |
| **Primary fails** | Secondary elected, no data loss | New primary takes over immediately |

### Network Partition

```
Nodes 1-2 (majority) ──── Network Partition ──── Node 3 (minority)

Quorum present:
- Nodes 1-2 continue as primary
- Node 3 stops accepting writes (fenced)

After partition heals:
- Node 3 resynchronizes from nodes 1-2
```

### Repository Corruption

```bash
# Detect (automated health checks)
./bin/iq-cli-server check-integrity

# If detected:
# 1. Failover to secondary (which may be clean)
./bin/iq-cli-server promote-secondary

# 2. Repair corrupted node offline
./mvnw -pl iq-rdf4j exec:java \
  -Dexec.mainClass="systems.symbol.rdf.RepositoryRepair" \
  -Dexec.args="/opt/iq/repositories/default"

# 3. Rejoin cluster
./bin/iq
```

## Performance Tuning

### Network Configuration

```bash
# Increase TCP buffer sizes (Linux)
sysctl -w net.core.rmem_max=134217728
sysctl -w net.core.wmem_max=134217728
sysctl -w net.ipv4.tcp_rmem="4096 87380 134217728"
sysctl -w net.ipv4.tcp_wmem="4096 65536 134217728"

# Disable Nagle's algorithm (reduce latency)
sysctl -w net.ipv4.tcp_nodelay=1
```

### Connection Pool Tuning

```turtle
iq:repo-cluster-tuned
a iq:RemoteRepository ;
iq:connectionPool [
iq:coreSize 20 ;
iq:maxSize 50 ;
iq:queueSize 100 ;
iq:keepAliveSecs 300 ;
iq:connectionTimeoutMs 10000 ;
iq:readTimeoutMs 30000
] ;
iq:connectionValidator [
iq:validateOnBorrow true ;
iq:validateOnReturn true ;
iq:validationQuery "ASK WHERE { ?s ?p ?o } LIMIT 1"
] .
```

## Next Steps

1. **[Monitoring](11-MONITORING.md)** — Health checks and alerting
2. **[Operations](10-OPERATIONS.md)** — Day-to-day cluster management
3. **[Disaster Recovery](10-OPERATIONS.md#backup-and-recovery)** — Full recovery procedures
