# CONNECTORS — How to integrate with your systems

IQ comes with 20+ pre-built connectors. Connectors read from and write to external systems, bringing data into IQ's knowledge graph or pushing decisions out.

## Quick reference

| System | Connector | What it does | Setup time |
|---|---|---|---|
| **Cloud** | | | |
| AWS | `iq-connect-aws` | S3, EC2, IAM, RDS, CloudWatch | 15 min |
| Azure | `iq-connect-azure` | VMs, Storage, SQL, Cosmos | 15 min |
| Google Cloud | `iq-connect-gcp` | Compute, Storage, BigQuery | 15 min |
| **SaaS** | | | |
| Slack | `iq-connect-slack` | Send/read messages, channel management | 10 min |
| GitHub | `iq-connect-github` | Issues, repos, PRs, releases | 10 min |
| Jira | `iq-connect-jira` | Tickets, projects, workflows | 15 min |
| Salesforce | `iq-connect-salesforce` | Accounts, contacts, opportunities | 20 min |
| Stripe | `iq-connect-stripe` | Customers, subscriptions, invoices | 10 min |
| Datadog | `iq-connect-datadog` | Metrics, monitors, incidents | 10 min |
| **Data** | | | |
| Databases | `iq-connect-jdbc` | Any SQL database (PostgreSQL, MySQL, etc.) | 5 min |
| Snowflake | `iq-connect-snowflake` | Tables, views, results of queries | 15 min |
| Databricks | `iq-connect-databricks` | Lakehouse tables, Delta lake | 15 min |
| Parquet | `iq-connect-parquet` | Files from S3, GCS, ADLS | 10 min |
| **Containers** | | | |
| Kubernetes | `iq-connect-k8s` | Pods, deployments, services | 10 min |
| Docker | `iq-connect-docker` | Running containers, images | 5 min |
| **Messaging** | | | |
| Kafka | `iq-connect-kafka` | Topics, messages, consumer groups | 15 min |
| **Docs** | | | |
| Confluence | `iq-connect-confluence` | Pages, spaces, attachments | 10 min |
| Google Drive | `iq-connect-google-apps` | Docs, sheets, slides | 10 min |
| Office 365 | `iq-connect-office-365` | Word, Excel, Teams, Mail | 10 min |
| **Infrastructure** | | | |
| DigitalOcean | `iq-connect-digitalocean` | Droplets, volumes, databases | 10 min |

---

## How connectors work

### Read mode
```
External System (Slack, AWS, GitHub)
          ↓
    IQ Connector (reads)
          ↓
  IQ Knowledge Graph (facts are created)
```

Example: "Fetch the last 10 Slack messages in #engineering and add them to our knowledge graph"

### Write mode
```
IQ Agent (decides to send a message)
          ↓
    IQ Connector (writes)
          ↓
External System (Slack receives a message)
```

Example: "Send a message to #incidents with a summary of this outage"

### Sync mode
Periodic read, every N hours/days. Keep your knowledge fresh.

```bash
# Every 6 hours, fetch latest from Slack
iq> connector sync slack-connector every 6 hours
```

---

## Example 1: Slack connector (send/read messages)

### Setup

**1. Get Slack credentials**
- Go to https://api.slack.com/apps
- Create a new app for your workspace
- Get the "Bot Token" (looks like `xoxb-...`)

**2. Configure IQ**

```bash
# Set environment variable
export SLACK_BOT_TOKEN=xoxb-your-token-here

# Or add to .iq/config.properties
iq.connector.slack.token=xoxb-your-token-here
iq.connector.slack.channels=engineering,incidents,general
```

**3. Import and activate**

```bash
./bin/setup-connectors examples/connectors/slack/
./bin/start-api
```

### Use case: Alert bot

When an incident happens, send a summary to Slack:

```bash
curl -X POST http://localhost:8080/agent/trigger \
  -d '{
    "intent": "post-incident-alert",
    "incident_id": "inc-12345",
    "channels": ["#incidents", "#incident-on-call"]
  }'
```

IQ will:
1. Get incident details from knowledge graph
2. Get affected customers from Salesforce
3. Format a professional summary
4. Send to both channels
5. Log the action

### Use case: Capture feedback

Read messages from `#feedback`, analyze them, add to knowledge:

```bash
iq> connector read slack #feedback \
      --into-realm product \
      --time-window "last 7 days"
```

IQ will:
1. Fetch the last 7 days of messages in #feedback
2. Extract sentiment, topics, mentions of features
3. Add to knowledge graph with metadata
4. You can then query: "What features are customers asking for?"

---

## Example 2: AWS connector (read/manage infrastructure)

### Setup

**1. Get AWS credentials**
```bash
export AWS_ACCESS_KEY_ID=AKIAI...
export AWS_SECRET_ACCESS_KEY=j+K2...

# Or use IAM role if running on EC2
```

**2. Configure which AWS services to monitor**

```bash
./bin/setup-connectors examples/connectors/aws/

# Edit .iq-templates/connectors/aws-config.ttl
# Choose: S3, EC2, RDS, CloudWatch, Costs, etc.
```

**3. Start and sync**

```bash
./bin/start-api
iq> connector sync aws-connector every 12 hours
```

### Use case: Cost anomaly detection

Sync AWS billing data, then run a SPARQL query:

```sparql
SELECT ?service ?cost ?month ?trend WHERE {
  ?cost-record a :AWSCost ;
    :service ?service ;
    :amount ?cost ;
    :month ?month ;
    :trend ?trend .
  FILTER (?cost > 2000 && ?trend = "spike")
}
ORDER BY DESC(?cost)
```

Result: "S3 costs doubled in March. Likely cause: new data ingestion pipeline."

### Use case: Cross-account compliance check

Is every EC2 instance backed up? Are all databases encrypted?

```bash
iq> query examples/queries/aws-compliance.sparql
```

Returns violations + affected resources. You can auto-remediate:

```bash
iq> agent run enable-backup \
      --filter "aws-instances WHERE backup = false"
```

---

## Example 3: GitHub connector (track development)

### Setup

**1. Get a GitHub token**
- Go to https://github.com/settings/tokens
- Create token with `repo` and `admin:repo_hook` scopes
- Copy the token

**2. Configure**

```bash
export GITHUB_TOKEN=github_pat_...

./bin/setup-connectors examples/connectors/github/
```

**3. Sync**

```bash
./bin/start-api
iq> connector sync github-connector every 4 hours
```

### Use case: Who is slowing down shipping?

Find the most-blocked issues and who is blocking them:

```bash
iq> query examples/queries/gh-blocking-issues.sparql

Result:
Issue | Blocker | Reason | Opened | Age
------|---------|--------|--------|----
#5678 | qa-team | waiting for test results | Mar 15 | 16 days
#5679 | ops | approval pending | Mar 28 | 3 days
#5680 | design | design review | Mar 20 | 11 days
```

### Use case: Release readiness

Before a release, check:
- Are all PRs merged to main?
- Are builds green?
- Are there critical open issues?

```bash
iq> agent run check-release-readiness --version 2.1.0

Result:
✓ All PRs merged
✓ Builds green (123 checks passed)
✓ Changelog updated
⚠ 1 critical issue still open: gh-5678 (assigned to alice)
→ RECOMMENDATION: Block release, contact alice
```

---

## Example 4: SQL database connector (read/write data)

Any SQL database works: PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, Snowflake, etc.

### Setup

**1. Create connection string**

```bash
export DATABASE_URL=postgresql://user:pass@localhost:5432/mydb
```

**2. Configure which tables to sync**

```bash
./bin/setup-connectors examples/connectors/jdbc/

# Edit .iq-templates/connectors/jdbc-config.ttl
# Specify tables: customers, orders, invoices, etc.
```

**3. Start**

```bash
./bin/start-api
iq> connector sync jdbc-connector every 1 hour
```

---

## All connectors: step-by-step

Every connector follows the same 3 steps:

1. **Get credentials**
   - API token, password, connection string, or credentials file
   - Set as environment variable or in `.iq/config.properties`

2. **Configure which data to sync**
   - Edit `.iq-templates/connectors/<name>-config.ttl`
   - Choose specific services, tables, channels, buckets, etc.
   - Set sync frequency (real-time, hourly, daily, manual)

3. **Sync and use**
   ```bash
   iq> connector sync <name> 
   # OR: iq> connector read <name> --into-realm my-project
   # Then query using SPARQL
   ```

---

## Advanced: Combining connectors

### Scenario: Track cloud costs AND customer health

```bash
# Sync AWS costs
iq> connector sync aws-connector

# Sync Salesforce accounts + revenue
iq> connector sync salesforce-connector

# Sync customer support tickets from Jira
iq> connector sync jira-connector

# Now query: "Which high-value customers have high support tickets?"
iq> query examples/queries/at-risk-customers.sparql

Result:
Customer | Revenue | Open Tickets | Trend
---------|---------|--------------|-------
Acme Inc | $2.3M   | 8 critical   | ↑ increasing
Beta LLC | $1.1M   | 5 critical   | ↑ increasing
Gamma Co | $800k   | 1 critical   | ↓ decreasing
```

### Scenario: Deploy to production after checklist

```bash
# Sync GitHub
iq> connector sync github-connector

# Sync Datadog (metrics)
iq> connector sync datadog-connector

# Sync PagerDuty (on-call)
iq> connector sync pagerduty-connector

# Run agent: "Is it safe to deploy?"
iq> agent run is-safe-to-deploy \
      --app myapp \
      --environment production

Agent checks:
✓ Main branch is green (all tests pass)
✓ Datadog metrics are healthy
✓ On-call engineer is awake (no sleep deficit)
✓ No critical incidents open
✓ Deploy window is during business hours

→ DECISION: DEPLOY
→ ACTION: Trigger deployment pipeline
```

---

## Troubleshooting connectors

| Problem | Solution |
|---|---|
| "Connector not found" | Did you run `./bin/setup-connectors`? |
| "Authorization failed" | Check your API token/credentials |
| "No data synced" | Check connector logs: `iq> connector logs <name>` |
| "Sync is slow" | Reduce scope (fewer tables/channels) or increase interval |
| "Memory usage is high" | Connectors are loading too much data. Reduce sync scope. |

---

## Next steps

- **Build with connectors:** See [USECASES.md](USECASES.md#3-multi-connector-orchestration-query-across-your-entire-tech-stack)
- **Advanced orchestration:** [Agent Setup](AGENTS.md)
- **Deploy connectors:** [Docker & Cloud](DOCKER.md)
- **Debug issues:** [FAQ](FAQ.md)

