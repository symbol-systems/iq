# Connectors Guide

Connectors are IQ's way of plugging external systems into your knowledge graph. They handle authentication, data sync, and event flow.

## Available Connectors

| Connector | Purpose | Status | Docs |
|-----------|---------|--------|------|
| **iq-connect-aws** | EC2, S3, RDS, Lambda, CloudWatch | ✅ Production | [AWS Setup](setup/aws.md) |
| **iq-connect-azure** | Azure VMs, Blob Storage, SQL DB | ✅ Production | [Azure Setup](setup/azure.md) |
| **iq-connect-slack** | Send/receive messages, reactions | ✅ Production | [Slack Setup](setup/slack.md) |
| **iq-connect-github** | Issues, PRs, workflows, code changes | ✅ Production | [GitHub Setup](setup/github.md) |
| **iq-connect-gcp** | GCP Compute, Storage, BigQuery | ✅ Production | [GCP Setup](setup/gcp.md) |
| **iq-connect-k8s** | Kubernetes clusters, workloads | ✅ Beta | [K8s Setup](setup/k8s.md) |
| **iq-connect-databricks** | Data warehouse queries, ML experiments | ✅ Beta | [Databricks Setup](setup/databricks.md) |
| **iq-connect-snowflake** | Snowflake SQL + database sync | ✅ Beta | [Snowflake Setup](setup/snowflake.md) |
| **iq-connect-salesforce** | Accounts, opportunities, contacts | 🔄 In Review | [Salesforce Setup](setup/salesforce.md) |
| **iq-connect-datadog** | Logs, metrics, APM, alerts | 🔄 In Review | [Datadog Setup](setup/datadog.md) |

And more: Google Workspace, Confluence, Docker, Stripe, DigitalOcean...

---

## Quick Start: Add a Connector

### 1. Enable the connector in your POM
```xml
<!-- In iq-apis/pom.xml or your module -->
<dependency>
<groupId>systems.symbol</groupId>
<artifactId>iq-connect-slack</artifactId>
<version>${project.version}</version>
</dependency>
```

### 2. Configure credentials
Connectors use IQ's vault system. Place credentials in `.iq/vault/`:

```bash
# Example: Slack token
mkdir -p .iq/vault
echo "xoxb-XXXXXXXXXXXXX-XXXXXXXXXXXXX-XXXXXXXXXXXXXXX" > .iq/vault/slack-token.secret
```

Or use environment variables:
```bash
export IQ_CONNECTOR_SLACK_TOKEN=xoxb-XXXXXXXXXXXXX...
./bin/start-api
```

### 3. Model the integration in RDF
Create `config/connectors.ttl`:
```turtle
@prefix iq: <http://systems.symbol/iq/> .
@prefix slack: <http://systems.symbol/connectors/slack/> .

slack:my-workspace a slack:Workspace ;
slack:token "secret:vault/slack-token" ;
slack:team-id "T123ABC" ;
slack:enable-events true .

slack:support-channel a slack:Channel ;
slack:name "#support" ;
slack:sync-interval "PT1M"^^xsd:duration .
```

### 4. Use the connector in workflows
```turtle
# In workflows.ttl
workflow:escalate-incident a iq:Workflow ;
iq:trigger incident:High ;
iq:step [
iq:action slack:Notify ;
iq:channel slack:support-channel ;
iq:message "🚨 Critical incident: {{incident.name}}"
] .
```

### 5. Query via MCP
```bash
# Your LLM now has a tool: send_slack_message
curl -X POST http://localhost:8080/mcp/tools/send_slack_message/execute \
  -H "Content-Type: application/json" \
  -d '{"channel": "#alerts", "message": "System healthy"}'
```

---

## Common Connector Patterns

### Event-Driven Sync
Connectors can pull changes when events occur:
```turtle
connector:aws-cloudwatch a iq:EventSource ;
iq:event-type "AWS:CloudWatch:Alarm" ;
iq:handler [
iq:action iq:UpdateKnowledgeGraph ;
iq:template monitoring:alarm-template
] .
```

### Periodic Sync
Pull data on a schedule:
```turtle
connector:salesforce a iq:Connector ;
iq:sync-interval "PT15M"^^xsd:duration ;
iq:resource [
iq:type "Account" ;
iq:query "SELECT * FROM Account WHERE LastModifiedDate > {{last_sync}}"
] .
```

### Request-Response (Pull on Demand)
Fetch data when explicitly queried:
```sparql
SELECT DISTINCT ?customer_id ?name ?email {
  ?c a crm:Customer ;
 id:external-id ?customer_id ;
 connector:pull-from connector:salesforce .
  # IQ fetches from Salesforce when query executes
}
```

---

## Authentication & Credentials

IQ uses a **Vault** for secure credential storage:

### Option A: File-based Vault
```bash
.iq/
├── vault/
│   ├── slack-token.secret
│   ├── aws-key-id.secret
│   ├── aws-secret-key.secret
│   └── db-password.secret
├── config/
│   ├── connectors.ttl
│   └── repositories.yaml
```

Vault is encrypted with a master key (set via `IQ_VAULT_KEY` or `.iq/vault/master.key`).

### Option B: Environment Variables
```bash
export IQ_CONNECTOR_SLACK_TOKEN=xoxb-...
export IQ_CONNECTOR_AWS_KEY_ID=AKIA...
export IQ_CONNECTOR_AWS_SECRET_KEY=...
./bin/start-api
```

### Option C: AWS Secrets Manager / HashiCorp Vault
For production:
```turtle
connector:slack a slack:Connector ;
iq:credential-source iq:AWSSecretsManager ;
iq:secret-key "prod/iq/slack-token" .
```

---

## Debugging Connectors

### Check connector status via REST API
```bash
curl http://localhost:8080/api/connectors | jq .
```

Response:
```json
{
  "connectors": [
{
  "name": "slack",
  "status": "connected",
  "last_sync": "2024-03-31T12:34:56Z",
  "last_error": null,
  "metrics": {
"syncs_total": 1523,
"sync_duration_ms": 234,
"errors": 0
  }
}
  ]
}
```

### View connector logs
```bash
./bin/start-api 2>&1 | grep -i connector
# or
docker-compose logs -f iq-apis | grep connector
```

### Test a connector
```bash
# Example: test Slack connection
curl -X POST http://localhost:8080/api/test-connector/slack \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

---

## Building Your Own Connector

Connectors are Maven modules in `iq-connect/iq-connect-{system}/`:

```
iq-connect-myservice/
├── pom.xml
├── README.md
├── src/main/java/
│   └── systems/symbol/connectors/myservice/
│   ├── MyServiceConnector.java   (main service interface)
│   ├── MyServiceConfig.java  (RDF-backed config)
│   └── MyServiceEventListener.java   (event handling)
├── src/main/resources/
│   └── myservice-schema.ttl  (RDF ontology)
└── src/test/java/
└── MyServiceConnectorTest.java
```

**Reference:** See `iq-connect-template/` for a complete example.

---

## Production Checklist

Before deploying connectors to production:

- [ ] Credentials are in vault, never in code
- [ ] Connector has error handling & retry logic
- [ ] All APIs have request/response timeouts
- [ ] Sync intervals are appropriate (avoid rate limits)
- [ ] Monitoring is enabled (sync metrics, errors)
- [ ] Fallback behavior is defined (what if API is down?)
- [ ] Security: TLS for all external connections
- [ ] Audit logging: all connector actions are logged to RDF

---

## Next Steps

1. Choose a connector from the list above
2. Follow the connector-specific setup guide
3. Check the examples in `examples/usecases/` that use that connector
4. Deploy with confidence!

**Need help?** See [FAQ.md](FAQ.md) or open an issue.
