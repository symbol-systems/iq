---
title: CLI Command Reference
audience: ["developer", "devops"]
sections: ["init", "realms", "queries", "agents", "connectors", "server", "cluster"]
---

# CLI Command Reference

Complete reference for `iq-cli`, `iq-cli-pro`, and `iq-cli-server` commands.

## Getting Help

```bash
./bin/iq-cli --help
./bin/iq-cli <command> --help
./bin/iq-cli-pro --help
./bin/iq-cli-server --help
```

## Global Options

Available with all commands:

```bash
./bin/iq-cli [GLOBAL_OPTIONS] <command> [COMMAND_OPTIONS]

--realm <name>  # Specify realm (default: prod)
--home <path>   # IQ home directory (default: ~/.iq)
--token <token> # API token for auth
--verbose, -v   # Verbose output
--format <format>   # Output format: json|yaml|table|text (default: table)
--timeout <seconds> # Command timeout
--no-color  # Disable colored output
--log-level <level> # DEBUG|INFO|WARN|ERROR
```

## Realm Management

### List Realms

```bash
./bin/iq-cli realms list

# Output:
# ┌─────────┬──────────────────────────┬─────────────┐
# │ Name│ Description  │ Status  │
# ├─────────┼──────────────────────────┼─────────────┤
# │ prod│ Production environment   │ HEALTHY │
# │ staging │ Staging environment  │ HEALTHY │
# │ dev │ Development environment  │ UNHEALTHY   │
# └─────────┴──────────────────────────┴─────────────┘

# JSON output
./bin/iq-cli realms list --format json
```

### Show Realm Details

```bash
./bin/iq-cli realms describe prod

# Output:
# Realm: prod
# Description: Production environment
# Status: HEALTHY
# Repository: prod-repo (Remote, 5.2GB)
# Triples: 5,234,891
# LLM: gpt-4
# Agents: 8
# Connectors: 5
# Last Synced: 2024-01-15T10:30:00Z
```

### Create Realm

```bash
./bin/iq-cli realms create \
  --name "customer-a" \
  --description "Dedicated realm for Customer A" \
  --repository "native" \
  --storage-path "./repositories/customer-a" \
  --llm-config "gpt-4" \
  --isolation "strict"

# Verify creation
./bin/iq-cli realms describe customer-a
```

### Delete Realm (Careful!)

```bash
./bin/iq-cli realms delete staging \
  --confirm \
  --backup-first \
  --backup-path "/backups/staging-$(date +%s).tar.gz"
```

## Semantic Queries

### SPARQL SELECT Query

```bash
./bin/iq-cli sparql "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10"

# With realm
./bin/iq-cli sparql --realm prod "SELECT ..."

# With timeout
./bin/iq-cli sparql --timeout 60 "SELECT ..."

# JSON output
./bin/iq-cli sparql --format json "SELECT ..."
```

### SPARQL ASK Query

```bash
./bin/iq-cli sparql "ASK WHERE { ?s ?p ?o }"

# Response: true | false
```

### SPARQL UPDATE

```bash
./bin/iq-cli sparql --update "INSERT DATA { 
  <urn:incident:123> a iq:Incident ;
iq:severity \"critical\" ;
iq:status \"open\" .
}"
```

### Execute SPARQL from File

```bash
./bin/iq-cli script queries/find-failing-services.sparql

# Output results in default format (table)
./bin/iq-cli script queries/find-failing-services.sparql --format json > results.json
```

### Query Timing

```bash
./bin/iq-cli sparql --timing "SELECT ?s WHERE { ?s ?p ?o } LIMIT 100000"

# Output:
# ┌──────────────────┬─────────────┐
# │ Metric   │ Value   │
# ├──────────────────┼─────────────┤
# │ Query Time   │ 342ms   │
# │ Result Count │ 100,000 │
# │ Rows/sec │ 292,400 │
# └──────────────────┴─────────────┘
```

## Agent Management

### List Agents

```bash
./bin/iq-cli agents list

# Output:
# ┌──────────────────────┬───────────┬──────────┐
# │ Agent│ States│ Enabled? │
# ├──────────────────────┼───────────┼──────────┤
# │ incident-response│ 8 │ yes  │
# │ deployment-automation│ 5 │ yes  │
# │ capacity-planning│ 4 │ no   │
# └──────────────────────┴───────────┴──────────┘
```

### Describe Agent

```bash
./bin/iq-cli agents describe incident-response

# Output:
# Agent: incident-response
# Description: Automated incident handling workflow
# States: 8
#   - initial
#   - alert_received
#   - notification_sent
#   - owner_acknowledged
#   - escalated
#   - mitigated
#   - post_mortem
#   - closed
# Transitions: 15
# Enabled: yes
```

### Trigger Agent

```bash
./bin/iq-cli agents trigger incident-response \
  --intent "declare_incident" \
  --param severity=critical \
  --param service="payment-api" \
  --param description="High error rate"

# Response:
# Execution ID: exec_abc123
# Status: in_progress
# Current State: notification_sent
# Next Action: wait for acknowledgment

# Check status
./bin/iq-cli agents status exec_abc123
```

### List Active Executions

```bash
./bin/iq-cli agents active

# Output:
# ┌────────────────┬───────────────┬──────────────────┬──────────┐
# │ Execution ID   │ Agent │ Current State│ Elapsed  │
# ├────────────────┼───────────────┼──────────────────┼──────────┤
# │ exec_abc123│ incident-resp │ notification_sent│ 00:02:14 │
# │ exec_def456│ deployment-au │ awaiting_approval│ 00:00:45 │
# └────────────────┴───────────────┴──────────────────┴──────────┘
```

## Connector Management

### List Connectors

```bash
./bin/iq-cli connectors list

# Output:
# ┌─────────────┬──────────────┬─────────┬──────────────────────┐
# │ Name│ Type │ Enabled │ Last Synced  │
# ├─────────────┼──────────────┼─────────┼──────────────────────┤
# │ aws-prod│ aws  │ yes │ 2024-01-15T10:30:00Z │
# │ k8s-prod│ kubernetes   │ yes │ 2024-01-15T10:32:00Z │
# │ slack-prod  │ slack│ yes │ 2024-01-15T10:31:00Z │
# │ github-org  │ github   │ yes │ 2024-01-15T10:25:00Z │
# └─────────────┴──────────────┴─────────┴──────────────────────┘
```

### Connector Status

```bash
./bin/iq-cli connectors status aws-prod

# Output:
# Connector: aws-prod
# Type: AWS
# Status: HEALTHY
# Last Sync: 2024-01-15T10:30:00Z
# Next Sync: 2024-01-15T11:00:00Z
# Resources Synced: 2,345
# Errors: 0
# Rate Limit: 50 req/sec (29 remaining)
```

### Test Connector

```bash
./bin/iq-cli connectors test aws-prod

# Output:
# Testing aws-prod...
# ✓ Credentials valid
# ✓ API reachable
# ✓ Can list EC2 instances (12 found)
# ✓ Can list S3 buckets (8 found)
# ✓ Sample query successful
# Status: HEALTHY
```

### Trigger Sync

```bash
./bin/iq-cli connectors sync aws-prod

# Without waiting
./bin/iq-cli connectors sync aws-prod --async

# Sync multiple
./bin/iq-cli connectors sync --all
```

## Server Management

### Start API Server

```bash
./bin/iq-cli-server server api start

# Or use launcher script
./bin/iq
```

### Stop Server

```bash
./bin/iq-cli-server server api stop

# Force stop (dangerous)
./bin/iq-cli-server server api stop --force
```

### Server Status

```bash
./bin/iq-cli-server server api status

# Output:
# API Server Status:
# Status: RUNNING
# PID: 12345
# Uptime: 5d 3h 12m
# Memory: 6.2GB / 16GB
# CPU: 2.1%
# Requests/sec: 145
# Avg Latency: 234ms
```

### Health Check

```bash
./bin/iq-cli-server health

# Detailed
./bin/iq-cli-server health --detailed

# Output:
# System Health: HEALTHY
# Components:
#   Database: HEALTHY
#   RDF Repository: HEALTHY
#   Cache: HEALTHY
#   Kafka: HEALTHY
```

### Server Logs

```bash
# View recent logs
./bin/iq-cli-server logs --lines 100

# Follow logs
./bin/iq-cli-server logs --follow

# Filter by level
./bin/iq-cli-server logs --level ERROR --lines 50

# Export logs
./bin/iq-cli-server logs --since "2024-01-15T00:00:00Z" --output logs.zip
```

## Cluster Management

### List Cluster Nodes

```bash
./bin/iq-cli-server cluster list

# Output:
# ┌──────────────────────────┬──────────┬─────────┬─────────┐
# │ Node ID  │ Role │ Status  │ Synced? │
# ├──────────────────────────┼──────────┼─────────┼─────────┤
# │ node1.iq.internal:8081   │ LEADER   │ HEALTHY │ yes │
# │ node2.iq.internal:8081   │ FOLLOWER │ HEALTHY │ yes │
# │ node3.iq.internal:8081   │ FOLLOWER │ HEALTHY │ yes │
# └──────────────────────────┴──────────┴─────────┴─────────┘
```

### Node Status

```bash
./bin/iq-cli-server node-status node1.iq.internal

# Output:
# Node: node1.iq.internal
# Status: HEALTHY
# Role: LEADER
# Synced: yes
# Memory: 8.1GB / 16GB
# Repositories:
#   - prod-repo: SYNCED (5.2GB, 5.2M triples)
#   - staging-repo: SYNCED (1.2GB, 800K triples)
```

### Add Node to Cluster

```bash
./bin/iq-cli-server cluster add node4.iq.internal:8081

# Wait for sync
./bin/iq-cli-server cluster list
```

### Remove Node from Cluster

```bash
./bin/iq-cli-server cluster remove node3.iq.internal:8081 --confirm
```

### Check Replication Lag

```bash
./bin/iq-cli-server replication-lag

# Output:
# ┌──────────────────────────┬────────────┐
# │ Node │ Lag│
# ├──────────────────────────┼────────────┤
# │ node1 (Primary)  │ 0ms│
# │ node2 (Secondary)│ 145ms  │
# │ node3 (Secondary)│ 234ms  │
# └──────────────────────────┴────────────┘
```

## Secrets Vault Management

### Set Secret

```bash
./bin/iq-cli vault set \
  --name "openai/api-key" \
  --value "sk-..." \
  --realm prod
```

### Get Secret

```bash
./bin/iq-cli vault get openai/api-key
# Output: sk-...
```

### List Secrets

```bash
./bin/iq-cli vault list

# Output:
# openai/api-key
# aws/access-key-id
# aws/secret-access-key
# rdf4j/username
# rdf4j/password
```

### Delete Secret

```bash
./bin/iq-cli vault delete openai/api-key --confirm
```

### Rotate Secrets

```bash
./bin/iq-cli vault rotate openai/api-key --backup-old
```

## Repository Operations

### Repository Info

```bash
./bin/iq-cli repositories info prod-repo

# Output:
# Repository: prod-repo
# Type: RemoteRepository
# Server: http://rdf4j-prod:8080/rdf4j-server
# Size: 5.2GB
# Triples: 5,234,891
# Last Indexed: 2024-01-15T02:00:00Z
# Index Status: CURRENT
```

### Statistics

```bash
./bin/iq-cli repositories stat prod-repo

# Output:
# Graph Statistics
# ├─ Total Triples: 5,234,891
# ├─ Subjects: 456,234
# ├─ Predicates: 1,245
# ├─ Objects: 2,123,456
# └─ Contexts: 8
```

### Verify Integrity

```bash
./bin/iq-cli repositories verify prod-repo

# Output:
# Verifying repository: prod-repo...
# ✓ Index integrity: VALID
# ✓ Triple counts: CONSISTENT
# ✓ Reference integrity: VALID
# Status: HEALTHY
```

### Compact Repository

```bash
./bin/iq-cli repositories compact prod-repo --confirm

# This may take a while for large repos
```

## Data Import/Export

### Export Graph

```bash
./bin/iq-cli export prod-repo --format nquads --output graph.nq

# Other formats: turtle, rdfxml, jsonld, trig
```

### Import Graph

```bash
./bin/iq-cli import prod-repo --file data.nq

# Verify import
./bin/iq-cli sparql "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }"
```

## Backup and Recovery

### Create Backup

```bash
./bin/iq-cli backup --realm prod --output backup.tar.gz

# Incremental backup (faster)
./bin/iq-cli backup --realm prod --incremental

# With verification
./bin/iq-cli backup --realm prod --verify
```

### List Backups

```bash
./bin/iq-cli backups list
```

### Restore Backup

```bash
./bin/iq-cli restore backup.tar.gz --realm prod

# To a different realm
./bin/iq-cli restore backup.tar.gz --realm prod-restored
```

## Troubleshooting Commands

### Check System Status

```bash
./bin/iq-cli status

# Or verbose
./bin/iq-cli status --verbose
```

### Diagnostic Bundle

```bash
./bin/iq-cli diagnose --output diag-$(date +%s).zip

# Includes: logs, config, metrics, system info
```

### Database Consistency Check

```bash
./bin/iq-cli check-consistency --realm prod --repair-if-needed
```

## iq-cli-pro Extensions

### Boot Lifecycle

```bash
./bin/iq-cli-pro boot --mode "auto-reconciliation"
```

### Run Script

```bash
./bin/iq-cli-pro run scripts/data-ingestion.sparql

# With parameters
./bin/iq-cli-pro run queries/parameterized.sparql \
  --param environment=prod \
  --param timeout=300
```

### Trigger Workflow

```bash
./bin/iq-cli-pro trigger event incident-declared \
  --data '{"severity":"critical","service":"api"}'
```

## Shell Completion

Enable bash/zsh auto-completion:

```bash
# Bash
source <(./bin/iq-cli completion bash)

# Zsh
source <(./bin/iq-cli completion zsh)
```

## Scripting Examples

### Loop Over Realms

```bash
for realm in $(./bin/iq-cli realms list --format json | jq -r '.[].name'); do
  echo "Processing realm: $realm"
  ./bin/iq-cli sparql --realm "$realm" "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }"
done
```

### Sync All Connectors

```bash
./bin/iq-cli connectors list --format json | \
  jq -r '.[].name' | \
  xargs -I {} ./bin/iq-cli connectors sync {}

# Wait for all syncs
./bin/iq-cli connectors list --format json | \
  jq 'map(select(.status != "SYNCED")) | length'
```

### Health Check Script

```bash
#!/bin/bash
./bin/iq-cli-server health || exit 1
./bin/iq-cli-server cluster list | grep -q "HEALTHY" || exit 1
./bin/iq-cli connectors list --format json | jq '.[] | select(.status != "HEALTHY")' | grep -q . && exit 1
echo "All systems HEALTHY"
```

## Next Steps

1. **[Operations](10-OPERATIONS.md)** — Common workflows
2. **[Troubleshooting](12-TROUBLESHOOTING.md)** — Common issues
