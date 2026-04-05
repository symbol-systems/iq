---
title: Troubleshooting Guide
audience: ["devops", "operator"]
sections: ["startup", "queries", "connectors", "cluster", "performance", "faq"]
---

# Troubleshooting Guide

Common issues and solutions for IQ operations.

## Server Startup Issues

### Server Won't Start - Java Version

**Error:**
```
Exception in thread "main" java.lang.UnsupportedClassVersionError: 
systems/symbol/iq/IQ has been compiled by a more recent version of the Java Runtime
```

**Solution:**

```bash
# Check Java version
java -version

# Should be 21 or newer
# If not, install Java 21:
# macOS: brew install java@21
# Linux: apt-get install openjdk-21-jdk
# Windows: choco install openjdk21

# Verify
java --version && javac --version
```

### Server Won't Start - Port Already in Use

**Error:**
```
Address already in use: bind(0.0.0.0:8080)
```

**Solution:**

```bash
# Find process using port
lsof -i :8080
# or
netstat -tlnp | grep 8080

# Kill process
kill -9 <PID>
# or use different port
export IQ_HTTP_PORT=8081
./bin/iq
```

### Server Won't Start - Repository Corruption

**Error:**
```
Failed to initialize repository: Transaction in progress
```

**Solution:**

```bash
# 1. Remove transaction lock
rm -f "${IQ_HOME}/repositories/default/txn-status"

# 2. Restart
./bin/iq

# 3. If still failing, verify integrity
./bin/iq-cli repositories verify prod-repo

# 4. If corrupt, restore from backup
tar -xzf /backups/repo-backup.tar.gz -C "${IQ_HOME}/"

# 5. Restart
./bin/iq
```

### Memory Issues

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**

```bash
# Increase heap size
export JAVA_OPTS="-Xmx8g"
./bin/iq

# Or set permanently in startup script
# Add to bin/iq:
export JAVA_OPTS="-Xmx${IQ_HEAP_SIZE:-8g}"

# Monitor heap usage
jps -v | grep iq-apis
# Use jvisualvm to attach debugger
```

## Query and SPARQL Issues

### Query Timeout

**Error:**
```
Query execution exceeded timeout: 30000ms
```

**Solution:**

```bash
# 1. Increase timeout
./bin/iq-cli sparql --timeout 60 "SELECT ..."

# 2. Optimize query - add LIMIT
./bin/iq-cli sparql --explain "SELECT ...LIMIT 10000"
# Review the explain plan

# 3. Check if indexes are being used
./bin/iq-cli repositories stat prod-repo

# 4. Add missing index
./bin/iq-cli repositories reindex prod-repo

# 5. If still slow, query might be inherently expensive
# - Break into smaller queries
# - Pre-compute results via CONSTRUCT
# - Use materialized views
```

### SPARQL Syntax Error

**Error:**
```
Malformed SPARQL query: syntax error at line 2
```

**Solution:**

```bash
# 1. Check SPARQL syntax (copy-paste into online validator)
# https://www.wikidata.org/wiki/Wikidata:Primer/SPARQL

# 2. Common mistakes:
# - Missing periods at end of triples
# - Undeclared prefixes (must have @prefix)
# - Incorrect property URIs (typos in namespace)

# 3. Test with simple query first
./bin/iq-cli sparql "SELECT ?s WHERE { ?s ?p ?o } LIMIT 1"

# 4. Build up complexity gradually
./bin/iq-cli sparql "SELECT ?s WHERE { ?s a rdf:type } LIMIT 1"
```

### No Results Returned

**Error:**
```
# Query returns 0 results, but data exists
./bin/iq-cli sparql "SELECT WHERE { ?s <urn:my:predicate> ?o }"
# Returns: no results
```

**Solution:**

```bash
# 1. Check data exists
./bin/iq-cli sparql "SELECT ?p WHERE { ?s ?p ?o } LIMIT 10"
# Review the predicates

# 2. Verify namespace URI
./bin/iq-cli sparql "SELECT ?p WHERE { ?s ?p ?o FILTER(CONTAINS(STR(?p), 'my')) }"

# 3. Check case sensitivity
# RDF URIs are case-sensitive!
# <urn:my:Predicate> != <urn:my:predicate>

# 4. Export sample data to inspect
./bin/iq-cli export prod-repo --format turtle --limit 100 | head -20

# 5. Use DESCRIBE to see all properties of a resource
./bin/iq-cli sparql "DESCRIBE <urn:resource:123>"
```

### INSERT/UPDATE Not Working

**Error:**
```
INSERT DATA { ... } executes but changes don't appear
```

**Solution:**

```bash
# 1. Verify update permissions
curl -i -X POST "http://localhost:8080/api/v1/realms/prod/sparql" \
  -H "Authorization: Bearer $IQ_API_TOKEN" \
  -d 'query=INSERT DATA { <urn:s> <urn:p> <urn:o> . }'

# 2. Check response - 403 = permission denied

# 3. Verify syntax (same as SELECT)
./bin/iq-cli sparql --update "DELETE DATA { <urn:s> <urn:p> <urn:o> . }"

# 4. If using remote repository, check credentials
./bin/iq-cli vault get secret:rdf4j/password

# 5. For large bulk updates, use LOAD or SPARQL files
./bin/iq-cli script update.sparql
```

## Connector Issues

### Connector Test Fails

**Error:**
```
./bin/iq-cli connectors test aws-prod
✗ Credentials invalid
```

**Solution:**

```bash
# 1. Check credentials in vault
./bin/iq-cli vault get secret:aws/access_key_id

# 2. Verify with AWS CLI directly
export AWS_ACCESS_KEY_ID="$(./bin/iq-cli vault get secret:aws/access_key_id)"
export AWS_SECRET_ACCESS_KEY="$(./bin/iq-cli vault get secret:aws/secret_access_key)"
aws ec2 describe-instances --region us-east-1

# 3. Check key expiry/rotation
# AWS keys need periodic rotation

# 4. Verify IAM permissions
# User must have descriptor policy for that service

# 5. Update vault with new credentials
./bin/iq-cli vault set secret:aws/access_key_id "NEW_KEY"
./bin/iq-cli vault set secret:aws/secret_access_key "NEW_SECRET"

# 6. Restart connector sync
./bin/iq-cli connectors sync aws-prod
```

### Connector Sync Hangs

**Error:**
```
Sync started 30 min ago, still running
./bin/iq-cli connectors status aws-prod
Status: SYNCING
Elapsed: 00:30:45
```

**Solution:**

```bash
# 1. Check connector logs
tail -f "${IQ_HOME}/logs/connector.log"

# 2. Check if API is responsive
./bin/iq-cli-server health

# 3. Try cancelling sync
./bin/iq-cli connectors sync aws-prod --cancel

# 4. Increase timeout
./bin/iq-cli connectors sync aws-prod --timeout 3600

# 5. Check rate limiting
# Service might be enforcing rate limits
# Add delay between requests

# 6. Check network connectivity
ping aws-endpoint.example.com
curl -v https://aws-endpoint.example.com/

# 7. If still hung, restart connector
./bin/iq-cli connectors stop aws-prod
sleep 10
./bin/iq-cli connectors start aws-prod
```

### Connector Sync Fails Intermittently

**Error:**
```
Sync sometimes fails, sometimes succeeds (flaky)
```

**Solution:**

```bash
# 1. Check for rate limiting
./bin/iq-cli connectors status aws-prod --verbose

# 2. Add backoff strategy
./bin/iq-cli connectors configure aws-prod \
  --backoff-multiplier 2.0 \
  --max-retries 5

# 3. Check for data validation errors
./bin/iq-cli connectors sync aws-prod --verbose 2>&1 | grep -E "Error|Invalid"

# 4. Reduce batch size
./bin/iq-cli connectors configure aws-prod --batch-size 100

# 5. Check for upstream service issues
# Monitor: https://status.aws.amazon.com/

# 6. Enable detailed logging
export CONNECTOR_LOG_LEVEL="DEBUG"
./bin/iq-cli connectors sync aws-prod
```

## Cluster Issues

### Node Cannot Join Cluster

**Error:**
```
Node failed to join cluster: Connection refused
./bin/iq-cli-server cluster add node4.iq.internal:8081
✗ Cannot connect to node4:8081
```

**Solution:**

```bash
# 1. Check if node is running
ssh node4 "ps aux | grep java | grep iq-apis"

# 2. Check network connectivity
ssh node4 "telnet $(hostname -f):8081"

# 3. Check firewall
ssh node4 "sudo netstat -tlnp | grep 8081"

# 4. If not listening, start server
ssh node4 "cd /opt/iq && ./bin/iq"

# 5. Verify cluster configuration
ssh node4 "grep IQ_CLUSTER_PEERS /usr/local/iq/env"

# 6. Check logs
ssh node4 "tail -50 /opt/iq/logs/iq.log | grep -i cluster"

# 7. Manual health check
curl http://node4:8080/health
```

### Lost Cluster Quorum

**Error:**
```
Cluster has lost quorum. Read-only mode activated.
./bin/iq-cli-server cluster list
✗ Error: Lost quorum (1 healthy nodes, 2 needed)
```

**Solution:**

```bash
# 1. Check cluster status on remaining nodes
for node in node1 node2 node3; do
  echo "=== $node ==="
  ssh $node "./bin/iq-cli-server cluster list"
done

# 2. Identify which nodes are down
./bin/iq-cli-server cluster list --verbose

# 3. Bring down remaining nodes gracefully
for node in node1 node2 node3; do
  ssh $node "./bin/iq-cli-server server api stop"
done

# 4. Check disk/memory on all nodes
for node in node1 node2 node3; do
  ssh $node "df -h; free -h"
done

# 5. Restart healthy nodes first (majority)
ssh node1 "./bin/iq"
sleep 30

ssh node2 "./bin/iq"
sleep 30

# 6. Rejoin remaining nodes
ssh node3 "./bin/iq"

# 7. Verify quorum restored
./bin/iq-cli-server cluster list
```

### Data Divergence Between Nodes

**Error:**
```
Nodes have different data:
node1: 5,234,891 triples
node2: 5,234,888 triples (3 missing)
```

**Solution:**

```bash
# 1. Check replication status
./bin/iq-cli-server replication-lag

# 2. If lag is high, wait for sync
watch -n5 "./bin/iq-cli-server replication-lag"

# 3. If lag persists, resync node from primary
ssh node2 "rm -rf /opt/iq/repositories/default"
ssh node2 "./bin/iq"  # Will resync from primary

# 4. Monitor resync progress
watch -n10 "ssh node2 './bin/iq-cli-server node-status node2.iq.internal | grep Synced'"

# 5. Once synced, verify consistency
./bin/iq-cli repositories stat prod-repo --check-consistency
```

## Performance Issues

### Slow Queries

**Error:**
```
SPARQL query takes 30+ seconds for routine query
```

**Solution:**

See [Query and SPARQL Issues](#query-and-sparql-issues) above.

### High CPU Usage

**Error:**
```
CPU at 90%+ even during light load
```

**Solution:**

```bash
# 1. Check what's consuming CPU
top -b -n 1 | grep java

# 2. Analyze with JFR
export JVM_ARGS="-XX:StartFlightRecording=duration=60s,filename=/tmp/iq-cpu.jfr"
./bin/iq
# Wait 60 seconds for recording

# 3. Use jfr print for analysis
jfr print --csv /tmp/iq-cpu.jfr > /tmp/iq-cpu.csv

# 4. Common causes:
# - Large garbage collection pauses
# - Inefficient query (missing index)
# - Connector doing heavy processing

# 5. Check GC logs
export JAVA_OPTS="-Xmx8g -Xlog:gc*:file=gc.log:time,uptime,level,tags"
./bin/iq

# 6. Tune GC settings
export JAVA_OPTS="-Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### High Memory Usage

**Error:**
```
Memory usage growing: 5GB -> 10GB -> 14GB (OOM imminent)
```

**Solution:**

```bash
# 1. Take heap dump
jmap -dump:live,format=b,file=heapdump.hprof <PID>

# 2. Analyze with Eclipse MAT
# https://www.eclipse.org/mat/

# 3. Common causes:
# - Large result sets not paginated
# - Connector caching too much data
# - Memory leak in plugin

# 4. Short term: restart server
./bin/iq-cli-server server api restart

# 5. Long term:
# - Limit query result sizes
# - Paginate large result sets
# - Check for memory leaks in logs

# 6. Configure memory limits
export JAVA_OPTS="-Xmx4g"  # Lower limit, fail fast
```

## Network and Connectivity

### Cannot Reach API Server

**Error:**
```
curl http://localhost:8080/health
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

**Solution:**

```bash
# 1. Check if server is running
ps aux | grep "java.*iq-apis"

# 2. Check if listening on correct port
netstat -tlnp | grep 8080

# 3. Check firewall
sudo ufw status
sudo iptables -L -n | grep 8080

# 4. Check if service crash
./bin/iq-cli-server health

# 5. Check logs for startup errors
tail -100 "${IQ_HOME}/logs/iq.log"

# 6. Try starting manually
./bin/iq

# 7. If locked file issue:
rm -f /opt/iq/.iq/runtime/iq.lock
./bin/iq
```

## FAQ

### Q: How do I upgrade IQ without downtime?

**A:** Use rolling update with cluster.

```bash
# Single node by node
for node in node1 node2 node3; do
  ssh $node "pkill -f iq-apis"
  ssh $node "cd /opt/iq && git pull && ./mvnw install"
  ssh $node "./bin/iq"
  sleep 60  # Wait for node to rejoin cluster
done
```

### Q: How do I migrate realms between servers?

**A:** Export and import via backup/restore.

```bash
# On source server
./bin/iq-cli backup --realm prod --output prod-migration.tar.gz

# Transfer file
scp prod-migration.tar.gz dest-server:/tmp/

# On destination server
./bin/iq-cli restore /tmp/prod-migration.tar.gz --realm prod-imported
```

### Q: How do I recover from a corrupted repository?

**A:** Restore from backup, or rebuild from data.

```bash
# Option 1: Restore from backup
./bin/iq-cli restore /backups/repo-backup.tar.gz --realm prod

# Option 2: Rebuild from exported data
./bin/iq-cli import prod-repo --file /backups/prod-export.nq

# Option 3: Rebuild from connector sync
for connector in $(./bin/iq-cli connectors list --format json | jq -r '.[].name'); do
  ./bin/iq-cli connectors sync "$connector"
done
```

### Q: How do I check why a connector isn't syncing?

**A:** Review connector logs and test connectivity.

```bash
# Check status
./bin/iq-cli connectors status <name> --verbose

# Test credentials
./bin/iq-cli connectors test <name>

# Check logs
grep "<name>" "${IQ_HOME}/logs/connector.log"

# Test API endpoint manually
curl -v -H "Authorization: Bearer $TOKEN" "https://api.endpoint.com/..."
```

### Q: How do I optimize costs?

**A:** Monitor spending, set limits, optimize queries.

```bash
# View costs
./bin/iq-cli metrics llm --period "this month"

# Set monthly budget
# Edit .iq/config.ttl with iq:cost-policy

# Optimize expensive queries
./bin/iq-cli sparql --explain "SELECT ... LIMIT 10000"

# Use cheaper models where possible
# Update llm-config.ttl with cheaper providers
```

### Q: How do I debug authorization issues?

**A:** Check JWT claims and realm access.

```bash
# Test token
./bin/iq-cli auth test-token "user@example.com" --realm prod

# Check JWT claims
# Decode token at https://jwt.io/

# Check realm ACLs
./bin/iq-cli realms acl prod list

# Grant access
./bin/iq-cli realms acl prod --allow-user "user@example.com"
```

## Getting Help

- **GitHub Issues**: https://github.com/symbol-systems/iq/issues
- **Debug Bundle**: `./bin/iq-cli diagnose` (includes logs, config, metrics)
- **Enable Debug Logging**: `export IQ_LOG_LEVEL=DEBUG`

## Next Steps

1. **[Operations](10-OPERATIONS.md)** — Common tasks
2. **[Monitoring](11-MONITORING.md)** — Health and metrics
3. **[Clustering](08-CLUSTERING.md)** — HA and scaling
