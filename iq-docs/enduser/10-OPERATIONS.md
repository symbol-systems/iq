---
title: Operations and Common Tasks
audience: ["devops", "operator"]
sections: ["daily-ops", "connectors", "realms", "data", "scaling"]
---

# Operations and Common Tasks

Day-to-day operational workflows for running IQ in production.

## Daily Checks

### Morning Health Check

```bash
#!/bin/bash
echo "=== IQ Health Check ==="

# 1. API server status
echo "1. API Server..."
./bin/iq-cli-server health || exit 1

# 2. Cluster status
echo "2. Cluster Status..."
./bin/iq-cli-server cluster list | grep -E "HEALTHY|LEADER"

# 3. Repository status
echo "3. Repository..."
./bin/iq-cli repositories info prod-repo | grep -E "Status|Triples"

# 4. Connector status
echo "4. Connectors..."
./bin/iq-cli connectors list | grep -E "OK|HEALTHY|ERROR"

# 5. Disk space
echo "5. Disk Space..."
df -h "${IQ_HOME}" | tail -1

echo "=== Check Complete ==="
```

Run daily or via cron:

```bash
0 7 * * * /opt/iq/health-check.sh | mail -s "IQ Health Report" ops@example.com
```

## Connector Operations

### Sync All Connectors

```bash
# Sync all connectors in parallel
./bin/iq-cli connectors sync --all --async

# Wait for completion
watch -n5 ./bin/iq-cli connectors list

# When all show "SYNCED"
for name in $(./bin/iq-cli connectors list --format json | jq -r '.[].name'); do
  status=$(./bin/iq-cli connectors status "$name" | grep "Last Sync")
  echo "$name: $status"
done
```

### Schedule Connector Syncs

Add to crontab:

```bash
# AWS connector: every hour
0 * * * * /opt/iq/bin/iq-cli connectors sync aws-prod

# Kubernetes: every 10 minutes (frequent changes)
*/10 * * * * /opt/iq/bin/iq-cli connectors sync k8s-prod

# Snowflake: twice daily
0 6,18 * * * /opt/iq/bin/iq-cli connectors sync snowflake-prod
```

### Fix Failed Connector Sync

```bash
# 1. Check error
./bin/iq-cli connectors status aws-prod --verbose

# 2. Verify credentials
./bin/iq-cli vault get secret:aws/access_key_id

# 3. Test connectivity
telnet aws-endpoint.example.com 443

# 4. Check API limits
aws ec2 describe-account-attributes --region us-east-1

# 5. Retry sync
./bin/iq-cli connectors sync aws-prod --verbose

# 6. Monitor progress
watch -n2 ./bin/iq-cli connectors status aws-prod
```

## Realm Management

### Create New Realm for Team

```bash
# 1. Create realm
./bin/iq-cli realms create \
  --name "team-engineering" \
  --description "Engineering team knowledge graph" \
  --repository "native" \
  --storage-path "./repositories/team-engineering" \
  --llm-config "gpt-4" \
  --isolation "standard"

# 2. Enable connectors for team
./bin/iq-cli realms configure team-engineering \
  --add-connector aws-prod \
  --add-connector k8s-prod \
  --add-connector github-org

# 3. Set up access control
./bin/iq-cli realms acl team-engineering \
  --allow-role "engineering:admin" \
  --allow-role "engineering:member"

# 4. Verify creation
./bin/iq-cli realms describe team-engineering
```

### Backup Realm Before Changes

```bash
# 1. Full backup
./bin/iq-cli backup \
  --realm prod \
  --output "backups/prod-$(date +%Y%m%d-%H%M%S).tar.gz" \
  --verify

# 2. Store off-site
gsutil cp "backups/prod-*.tar.gz" gs://iq-backups/

# 3. Keep local recent backups
find backups -name "prod-*.tar.gz" -mtime +30 -delete
```

### Restore from Backup

```bash
# 1. Stop server
./bin/iq-cli-server server api stop

# 2. Restore backup
./bin/iq-cli restore backups/prod-20240115-102000.tar.gz --realm prod

# 3. Verify integrity
./bin/iq-cli repositories verify prod-repo

# 4. Start server
./bin/iq-cli-server server api start

# 5. Smoke test
./bin/iq-cli sparql "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }"
```

## Data Operations

### Bulk Data Import

```bash
# 1. Prepare data file (Turtle, N-Quads, RDF/XML)
# data.nq contains your triples

# 2. Import
time ./bin/iq-cli import prod-repo --file data.nq --progress

# 3. Verify import
./bin/iq-cli sparql \
  "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }" \
  --realm prod

# 4. Check statistics
./bin/iq-cli repositories stat prod-repo

# 5. Reindex if needed
./bin/iq-cli repositories reindex prod-repo --async
```

### Clean Up Stale Data

```bash
# 1. Find old triples (example: >1 year old)
./bin/iq-cli sparql "
  SELECT DISTINCT ?s ?p ?o WHERE {
?s ?p ?o .
?s dcterms:modified ?modified .
FILTER (?modified < '2023-01-15'^^xsd:dateTime)
  }
  LIMIT 10000
"

# 2. Delete them
./bin/iq-cli sparql --update "
  DELETE WHERE {
?s ?p ?o .
?s dcterms:modified ?modified .
FILTER (?modified < '2023-01-15'^^xsd:dateTime)
  }
"

# 3. Verify cleanup
./bin/iq-cli repositories stat prod-repo
```

### Export Data Periodically

```bash
# Weekly export for backup/analysis
0 2 * * 0 /opt/iq/bin/iq-cli export prod-repo \
  --format jsonld \
  --output "exports/prod-export-$(date +\%Y\%m\%d).jsonld" && \
  gzip "exports/prod-export-*.jsonld"

# Keep 12 weeks of exports
find exports -name "*.jsonld.gz" -mtime +84 -delete
```

## User and Access Management

### Grant User Access to Realm

```bash
# 1. Create identity
./bin/iq-cli identity create \
  --username "user@example.com" \
  --name "Jane Doe" \
  --email "jane@example.com"

# 2. Add to group
./bin/iq-cli identity group add engineering --member "user@example.com"

# 3. Grant realm access
./bin/iq-cli realms acl prod \
  --allow-group "engineering"

# 4. Assign role
./bin/iq-cli identity role assign "user@example.com" \
  --role "user:prod" \
  --role "operator:prod"

# 5. Verify access
./bin/iq-cli auth test-token "user@example.com" --realm prod
```

### Revoke Access

```bash
./bin/iq-cli identity role revoke "user@example.com" \
  --role "operator:prod" \
  --confirm
```

## Agent Workflow Operations

### Trigger Time-Sensitive Workflow

```bash
# Example: Incident response
./bin/iq-cli agents trigger incident-response \
  --intent "declare_incident" \
  --param severity="critical" \
  --param service="payment-api" \
  --param description="15% error rate, customer impact" \
  --param on_call_email="oncall@example.com"

# Track execution
EXEC_ID="exec_abc123"  # From response
watch -n2 "./bin/iq-cli agents status $EXEC_ID"
```

### Monitor Agent Executions

```bash
# Active executions
./bin/iq-cli agents active --realm prod

# Failed executions (last 24 hours)
./bin/iq-cli agents executions --filter failed --since 24h

# Execution history for specific agent
./bin/iq-cli agents executions incident-response --limit 50
```

## Scaling Operations

### Add Node to Cluster

When adding capacity:

```bash
# 1. Provision new server (e.g., in AWS/K8S)

# 2. Install IQ
ssh newnode "cd /opt && git clone [...] && ./mvnw install"

# 3. Configure as cluster member
ssh newnode "export IQ_CLUSTER_NODE_ID=node4 && \
 export IQ_CLUSTER_PEERS=node1:8081,node2:8081,node3:8081"

# 4. Start IQ
ssh newnode "./bin/iq"

# 5. Add to cluster management
./bin/iq-cli-server cluster add node4.iq.internal:8081

# 6. Verify sync
watch -n5 "./bin/iq-cli-server cluster list | grep node4"
```

### Scale Repository Vertically

If repository is too large for single node:

```bash
# 1. Switch to remote repository (shared RDF4J server)

# 2. Update config.ttl
# Change: iq:repository iq:repo-native
# To: iq:repository iq:repo-remote

# 3. Export from old repo
./bin/iq-cli export prod-repo \
  --format nquads \
  --output /tmp/prod-export.nq

# 4. Create new remote repo
curl -X PUT http://rdf4j-server:8080/rdf4j-server/repositories/prod-large ...

# 5. Import Into new repo
./bin/iq-cli import prod-large --file /tmp/prod-export.nq

# 6. Update realm config
./bin/iq-cli realms configure prod --repository prod-large

# 7. Restart
./bin/iq-cli-server server api restart

# 8. Verify
./bin/iq-cli sparql "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }"
```

## Cost Optimization

### Monitor LLM Costs

```bash
# View token usage and costs
./bin/iq-cli metrics llm --period "last 30 days"

# Output:
# ProviderTokens  Cost
# openai-gpt4 1,234,567   $45.67
# groq-mixtral  456,789$4.50
# Total   $50.17
```

### Set Cost Budgets

```turtle
iq:cost-control
a iq:CostPolicy ;
iq:monthly_budget 5000 ;  # Monthly limit in dollars
iq:alert_threshold 0.8 ;  # Alert at 80%
iq:enforcement "strict" ; # Stop on exceed
iq:excluded_models (
"gpt-4"   # High-value models excluded
) ;
iq:rate_limits [
iq:tokens_per_minute 100000 ;
iq:tokens_per_hour 5000000
] .
```

### Optimize Query Costs

```bash
# Check expensive queries
./bin/iq-cli metrics sparql --top-expensive 10

# Explain query plan
./bin/iq-cli sparql --explain \
  "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 1000"

# The plan shows cost estimate before execution
```

## Maintenance Windows

### Planned Maintenance Checklist

```bash
# 1. Announce to users
echo "Scheduled maintenance 2024-01-20 02:00-03:00 UTC" | \
  mail -s "IQ Maintenance Notice" users@example.com

# 2. Backup production
./bin/iq-cli backup --realm prod --verify

# 3. Stop accepting new requests (graceful)
./bin/iq-cli-server server api drain --timeout 300

# 4. Wait for in-flight requests
watch -n5 "./bin/iq-cli-server server api requests --active"

# 5. Stop server
./bin/iq-cli-server server api stop

# 6. Perform maintenance (updates, config changes, etc.)
git pull
./mvnw clean install

# 7. Start server
./bin/iq-cli-server server api start

# 8. Health check
./bin/iq-cli-server health --detailed

# 9. Announce readiness
echo "IQ maintenance complete, system operational" | \
  mail -s "IQ Back Online" ops@example.com
```

## Troubleshooting Common Operational Issues

See [Troubleshooting Guide](12-TROUBLESHOOTING.md) for detailed debugging.

## Next Steps

1. **[Monitoring](11-MONITORING.md)** — Set up metrics and alerting
2. **[Clustering](08-CLUSTERING.md)** — Scale for high availability
3. **[Troubleshooting](12-TROUBLESHOOTING.md)** — Fix issues
